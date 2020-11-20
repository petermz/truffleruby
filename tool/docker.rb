class JT
  class Docker
    include Utilities

    DOCKER = ENV['DOCKER'] || 'docker'

    def docker(*args)
      command = args.shift
      case command
      when 'build'
        docker_build(*args)
      when nil, 'test'
        docker_test(*args)
      when 'print'
        puts dockerfile(*args, '--print')
      else
        abort "Unkown jt docker command #{command}"
      end
    end

    private def docker_build(*args)
      if args.first.nil? || args.first.start_with?('--')
        image_name = 'truffleruby-test'
      else
        image_name = args.shift
      end
      docker_dir = File.join(TRUFFLERUBY_DIR, 'tool', 'docker')
      File.write(File.join(docker_dir, 'Dockerfile'), dockerfile(*args))
      sh DOCKER, 'build', '-t', image_name, '.', chdir: docker_dir
    end

    private def docker_test(*args)
      distros = ['--ol7', '--ubuntu1804', '--ubuntu1604', '--fedora28']

      distros.each do |distro|
        puts '**********************************'
        puts '**********************************'
        puts '**********************************'
        distros.each do |d|
          print d
          print '     <---' if d == distro
          puts
        end
        puts '**********************************'
        puts '**********************************'
        puts '**********************************'

        docker 'build', distro, *args
      end
    end

    private def dockerfile(*args)
      require 'yaml'
      config = @config ||= YAML.load_file(File.join(TRUFFLERUBY_DIR, 'tool', 'docker-configs.yaml'))

      distro = 'ol7'
      install_method = nil
      rebuild_images = false
      rebuild_openssl = true
      basic_test = false
      full_test = false
      root = false
      print_only = false

      until args.empty?
        arg = args.shift
        case arg
        when '--ol7', '--ubuntu1804', '--ubuntu1604', '--fedora28'
          distro = arg[2..-1]
        when '--graalvm'
          install_method = :graalvm
          graalvm_tarball = args.shift
          graalvm_components = args.shift.split(':').map { |path| File.expand_path(path) }
        when '--standalone'
          install_method = :standalone
          standalone_tarball = args.shift
        when '--rebuild-images'
          rebuild_images = true
          native_component = args.shift
        when '--no-rebuild-openssl'
          rebuild_openssl = false
        when '--basic-test'
          basic_test = true
        when '--test'
          full_test = true
          test_branch = args.shift
        when '--root'
          root = true
        when '--print'
          print_only = true
        else
          abort "unknown option #{arg}"
        end
      end

      distro = config.fetch(distro)
      run_post_install_hook = rebuild_openssl

      packages = []
      packages << distro.fetch('locale')

      packages << distro.fetch('tar')
      packages << distro.fetch('specs') if full_test

      packages << distro.fetch('zlib')
      packages << distro.fetch('openssl')
      packages << distro.fetch('cext')

      proxy_vars = []
      %w[http_proxy https_proxy no_proxy].each do |var|
        value = ENV[var]
        proxy_vars << "ENV #{var}=#{value}" if value
      end

      lines = [
        "FROM #{distro.fetch('base')}",
        *proxy_vars,
        [distro.fetch('install'), *packages.compact].join(' '),
        *distro.fetch('set-locale'),
      ]

      lines << 'WORKDIR /test'

      lines << 'RUN useradd -ms /bin/bash test'
      lines << 'RUN chown test /test'
      lines << 'USER test' unless root

      unless print_only
        docker_dir = File.join(TRUFFLERUBY_DIR, 'tool', 'docker')
        FileUtils.rm_rf docker_dir
        Dir.mkdir docker_dir
      end

      check_post_install_message = [
          "RUN grep 'The Ruby openssl C extension needs to be recompiled on your system to work with the installed libssl' install.log",
          "RUN grep '/languages/ruby/lib/truffle/post_install_hook.sh' install.log"
      ]

      case install_method
      when :graalvm
        FileUtils.copy graalvm_tarball, docker_dir unless print_only
        graalvm_tarball = File.basename(graalvm_tarball)
        language_dir = graalvm_tarball.include?('java11') ? 'languages' : 'jre/languages'

        lines << "COPY #{graalvm_tarball} /test/"
        graalvm_base = '/test/graalvm'
        lines << "RUN mkdir #{graalvm_base}"
        lines << "RUN tar -zxf #{graalvm_tarball} -C #{graalvm_base} --strip-components=1"
        graalvm_bin = "#{graalvm_base}/bin"

        graalvm_components.each do |component|
          FileUtils.copy component, docker_dir unless print_only
          component = File.basename(component)
          lines << "COPY #{component} /test/"
          lines << "RUN #{graalvm_bin}/gu install --file /test/#{component} | tee -a install.log"
        end
        ruby_base = "#{graalvm_base}/#{language_dir}/ruby"
        ruby_bin = graalvm_bin

        lines.push(*check_post_install_message)
        lines << "RUN #{ruby_base}/lib/truffle/post_install_hook.sh" if run_post_install_hook
      when :standalone
        FileUtils.copy standalone_tarball, docker_dir unless print_only
        standalone_tarball = File.basename(standalone_tarball)
        lines << "COPY #{standalone_tarball} /test/"
        ruby_base = '/test/truffleruby-standalone'
        lines << "RUN mkdir #{ruby_base}"
        lines << "RUN tar -zxf #{standalone_tarball} -C #{ruby_base} --strip-components=1"
        ruby_bin = "#{ruby_base}/bin"
        lines << "RUN #{ruby_base}/lib/truffle/post_install_hook.sh" if run_post_install_hook
      else
        raise "Unknown install method: #{install_method}"
      end

      if full_test
        test_files = %w[
          spec
          test/truffle/compiler/pe
          versions.json
        ]

        unless print_only
          chdir(docker_dir) do
            raw_sh 'git', 'clone', '--branch', test_branch, TRUFFLERUBY_DIR, 'truffleruby-tests'
            test_files.each do |file|
              FileUtils.cp_r "truffleruby-tests/#{file}", '.'
            end
            FileUtils.rm_rf 'truffleruby-tests'
          end
        end
      end


      if rebuild_images
        if install_method == :graalvm
          FileUtils.copy native_component, docker_dir unless print_only
          native_component = File.basename(native_component)
          lines << "COPY #{native_component} /test/"
          lines << "RUN #{graalvm_bin}/gu install --file /test/#{native_component} | tee install.log"
          lines << "RUN #{graalvm_base}/bin/gu rebuild-images polyglot libpolyglot"
        else
          abort "can't rebuild images for a build not from local GraalVM components"
        end
      end

      lines << "ENV PATH=#{ruby_bin}:$PATH"

      configs = install_method == :graalvm ? %w[--native --jvm] : ['']

      configs.each do |c|
        lines << "RUN ruby #{c} --version"
      end

      if basic_test || full_test
        configs.each do |c|
          lines << "RUN cp -r #{ruby_base}/lib/gems /test/clean-gems"

          gem_install = "ruby #{c} -S gem install --no-document"
          lines << "RUN #{gem_install} color"
          lines << "RUN ruby #{c} -rcolor -e 'raise unless defined?(Color)'"

          lines << "RUN #{gem_install} oily_png"
          lines << "RUN ruby #{c} -roily_png -e 'raise unless defined?(OilyPNG::Color)'"

          lines << "RUN #{gem_install} unf"
          lines << "RUN ruby #{c} -runf -e 'raise unless defined?(UNF)'"

          lines << "RUN rm -rf #{ruby_base}/lib/gems"
          lines << "RUN mv /test/clean-gems #{ruby_base}/lib/gems"
        end
      end

      if full_test
        test_files.each do |path|
          file = File.basename(path)
          lines << "COPY --chown=test #{file} #{file}"
        end

        configs.each do |c|
          excludes = %w[fails slow]

          %w[:command_line :security :language :core :library :capi :library_cext :truffle :truffle_capi].each do |set|
            t_config = c.empty? ? '' : '-T' + c
            t_excludes = excludes.map { |e| '--excl-tag ' + e }.join(' ')
            lines << "RUN ruby spec/mspec/bin/mspec -t #{ruby_bin}/ruby #{t_config} #{t_excludes} #{set}"
          end
        end

        configs.each do |c|
          lines << "RUN ruby #{c} --experimental-options --engine.CompilationFailureAction=ExitVM --engine.TreatPerformanceWarningsAsErrors=all --engine.IterativePartialEscape  --engine.MultiTier=false pe/pe.rb || true"
        end
      end

      lines << 'CMD bash'

      lines.join("\n") + "\n"
    end
  end
end
