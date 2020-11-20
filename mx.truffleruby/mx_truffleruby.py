# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 2.0, or
# GNU General Public License version 2, or
# GNU Lesser General Public License version 2.1.

import os
import pipes
from os.path import join, exists, basename

import mx
import mx_sdk
import mx_subst
import mx_spotbugs

if 'RUBY_BENCHMARKS' in os.environ:
    import mx_truffleruby_benchmark  # pylint: disable=unused-import

_suite = mx.suite('truffleruby')
root = _suite.dir

def add_ext_suffix(name):
    """
    Adds the platform specific C extension suffix to a name
    """
    if mx.is_darwin():
        return name + '.bundle'
    else:
        return name + '.so'

mx_subst.results_substitutions.register_with_arg('extsuffix', add_ext_suffix)

# Utilities

class VerboseMx:
    def __enter__(self):
        self.verbose = mx.get_opts().verbose
        mx.get_opts().verbose = True

    def __exit__(self, exc_type, exc_value, traceback):
        mx.get_opts().verbose = self.verbose

# Project classes

class TruffleRubyBootstrapLauncherProject(mx.Project):
    def __init__(self, suite, name, deps, workingSets, theLicense, **kwArgs):
        super(TruffleRubyBootstrapLauncherProject, self).__init__(suite, name, subDir=None, srcDirs=[], deps=deps, workingSets=workingSets, d=suite.dir, theLicense=theLicense, **kwArgs)

    def launchers(self):
        result = join(self.get_output_root(), 'miniruby')
        yield result, 'TOOL', 'miniruby'

    def getArchivableResults(self, use_relpath=True, single=False):
        for result, _, prefixed in self.launchers():
            yield result, prefixed

    def getBuildTask(self, args):
        return TruffleRubyBootstrapLauncherBuildTask(self, args, 1)


class TruffleRubyBootstrapLauncherBuildTask(mx.BuildTask):
    def __str__(self):
        return "Generating " + self.subject.name

    def newestOutput(self):
        return mx.TimeStampFile.newest([result for result, _, _ in self.subject.launchers()])

    def needsBuild(self, newestInput):
        sup = super(TruffleRubyBootstrapLauncherBuildTask, self).needsBuild(newestInput)
        if sup[0]:
            return sup

        for result, _, _ in self.subject.launchers():
            if not exists(result):
                return True, result + ' does not exist'
            with open(result, "r") as f:
                on_disk = f.read()
            if on_disk != self.contents(result):
                return True, 'command line changed for ' + basename(result)

        return False, 'up to date'

    def build(self):
        mx.ensure_dir_exists(self.subject.get_output_root())
        for result, _, _ in self.subject.launchers():
            with open(result, "w") as f:
                f.write(self.contents(result))
            os.chmod(result, 0o755)

    def clean(self, forBuild=False):
        if exists(self.subject.get_output_root()):
            mx.rmtree(self.subject.get_output_root())

    def contents(self, result):
        java = mx.get_jdk().java
        classpath_deps = [dep for dep in self.subject.buildDependencies if isinstance(dep, mx.ClasspathDependency)]
        jvm_args = [pipes.quote(arg) for arg in mx.get_runtime_jvm_args(classpath_deps)]
        jvm_args.append('-Dorg.graalvm.language.ruby.home=' + root)
        main_class = 'org.truffleruby.launcher.RubyLauncher'
        ruby_options = [
            '--experimental-options',
            '--building-core-cexts',
            '--launcher=' + result,
            '--disable-gems',
            '--disable-rubyopt',
        ]
        command = [java] + jvm_args + [main_class] + ruby_options + ['"$@"']
        return "#!/usr/bin/env bash\n" + "exec " + " ".join(command) + "\n"

# Commands

def jt(*args):
    mx.log("\n$ " + ' '.join(['jt'] + list(args)) + "\n")
    mx.run(['ruby', join(root, 'tool/jt.rb')] + list(args))

def build_truffleruby(args):
    mx.command_function('sversions')([])
    jt('build')

def ruby_run_ruby(args):
    """run TruffleRuby (through tool/jt.rb)"""

    jt = join(root, 'tool/jt.rb')
    os.execlp(jt, jt, "ruby", *args)

def ruby_run_specs(ruby, args):
    with VerboseMx():
        jt('--use', ruby, 'test', 'specs', *args)

def ruby_testdownstream_hello(args):
    """Run a minimal Hello World test"""
    build_truffleruby([])
    jt('ruby', '-e', 'puts "Hello Ruby!"')

def ruby_testdownstream_aot(args):
    """Run tests for the native image"""
    if len(args) > 3:
        mx.abort("Incorrect argument count: mx ruby_testdownstream_aot <aot_bin> [<format>] [<build_type>]")

    aot_bin = args[0]
    fast = ['--excl-tag', 'slow']

    ruby_run_specs(aot_bin, [])

    # Run "jt test fast --native :truffle" to catch slow specs in Truffle which only apply to native
    ruby_run_specs(aot_bin, fast + [':truffle'])

def ruby_testdownstream_sulong(args):
    """Run C extension tests"""
    build_truffleruby([])
    # Ensure Sulong is available
    mx.suite('sulong')

    # Only what is not already tested in the GraalVM gates
    jt('test', 'mri', '--all-sulong')
    jt('test', 'cexts')

def ruby_spotbugs(args):
    """Run SpotBugs with custom options to detect more issues"""
    filters = join(root, 'mx.truffleruby', 'spotbugs-filters.xml')
    spotbugsArgs = ['-textui', '-low', '-longBugCodes', '-include', filters]
    if mx.is_interactive():
        spotbugsArgs.append('-progress')
    mx_spotbugs.spotbugs(args, spotbugsArgs)

def verify_ci(args):
    """Verify CI configuration"""
    mx.verify_ci(args, mx.suite('truffle'), _suite, 'common.json')

mx_sdk.register_graalvm_component(mx_sdk.GraalVmLanguage(
    suite=_suite,
    name='TruffleRuby license files',
    short_name='rbyl',
    dir_name='ruby',
    license_files=['LICENSE_TRUFFLERUBY.txt'],
    third_party_license_files=['3rd_party_licenses_truffleruby.txt'],
    dependencies=[],
    truffle_jars=[],
    support_distributions=[
        'truffleruby:TRUFFLERUBY_GRAALVM_LICENSES',
    ],
    priority=5,
))

mx_sdk.register_graalvm_component(mx_sdk.GraalVmLanguage(
    suite=_suite,
    name='TruffleRuby',
    short_name='rby',
    dir_name='ruby',
    standalone_dir_name='truffleruby-<version>-<graalvm_os>-<arch>',
    license_files=[],
    third_party_license_files=[],
    dependencies=['rbyl', 'Truffle', 'Truffle NFI', 'Sulong', 'LLVM.org toolchain'],
    standalone_dependencies={
        'Sulong': ('lib/sulong', ['bin/<exe:lli>']),
        'LLVM.org toolchain': ('lib/llvm-toolchain', []),
        'TruffleRuby license files': ('', []),
    },
    truffle_jars=[
        'truffleruby:TRUFFLERUBY',
        'truffleruby:TRUFFLERUBY-SHARED',
        'truffleruby:TRUFFLERUBY-ANNOTATIONS'
    ],
    boot_jars=[
        'truffleruby:TRUFFLERUBY-SERVICES'
    ],
    support_distributions=[
        'truffleruby:TRUFFLERUBY_GRAALVM_SUPPORT',
    ],
    provided_executables=[
        'bin/bundle',
        'bin/bundler',
        'bin/gem',
        'bin/irb',
        'bin/rake',
        'bin/rdoc',
        'bin/ri',
    ],
    launcher_configs=[
        mx_sdk.LanguageLauncherConfig(
            destination='bin/<exe:truffleruby>',
            jar_distributions=['truffleruby:TRUFFLERUBY-LAUNCHER'],
            main_class='org.truffleruby.launcher.RubyLauncher',
            build_args=[
                '--report-unsupported-elements-at-runtime',
                '-H:+DumpThreadStacksOnSignal',
                '-H:+DetectUserDirectoriesInImageHeap',
                '-H:+TruffleCheckBlackListedMethods',
            ],
            language='ruby',
            links=['bin/<exe:ruby>'],
            option_vars=[
                'RUBYOPT',
                'TRUFFLERUBYOPT'
            ]
        )
    ],
    post_install_msg="""
IMPORTANT NOTE:
---------------
The Ruby openssl C extension needs to be recompiled on your system to work with the installed libssl.
First, make sure TruffleRuby's dependencies are installed, which are described at:
  https://github.com/oracle/truffleruby/blob/master/README.md#dependencies
Then run the following command:
        ${graalvm_languages_dir}/ruby/lib/truffle/post_install_hook.sh""",
))

mx.update_commands(_suite, {
    'ruby': [ruby_run_ruby, ''],
    'build_truffleruby': [build_truffleruby, ''],
    'ruby_testdownstream_aot': [ruby_testdownstream_aot, 'aot_bin'],
    'ruby_testdownstream_hello': [ruby_testdownstream_hello, ''],
    'ruby_testdownstream_sulong': [ruby_testdownstream_sulong, ''],
    'ruby_spotbugs': [ruby_spotbugs, ''],
    'verify-ci' : [verify_ci, '[options]'],
})
