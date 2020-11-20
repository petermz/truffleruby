suite = {
    "mxversion": "5.258.2",
    "name": "truffleruby",

    "imports": {
        "suites": [
            {
                "name": "sulong",
                "subdir": True,
                "version": "60faf95321609669bcf0a01e0e134c3b7906e435",
                "urls": [
                    {"url": "https://github.com/oracle/graal.git", "kind": "git"},
                    {"url": "https://curio.ssw.jku.at/nexus/content/repositories/snapshots", "kind": "binary"},
                ]
            },
        ],
    },

    "licenses": {
        "EPL-2.0": {
            "name": "Eclipse Public License 2.0",
            "url": "https://opensource.org/licenses/EPL-2.0",
        },
        "BSD-simplified": {
            "name": "Simplified BSD License (2-clause BSD license)",
            "url": "http://opensource.org/licenses/BSD-2-Clause"
        },
        "MIT": {
            "name": "MIT License",
            "url": "http://opensource.org/licenses/MIT"
        },
    },

    "repositories": {
        "truffleruby-binary-snapshots": {
            "url": "https://curio.ssw.jku.at/nexus/content/repositories/snapshots",
            "licenses": [
                "EPL-2.0",          # JRuby (we're choosing EPL out of EPL,GPL,LGPL)
                "BSD-simplified",   # MRI
                "BSD-new",          # Rubinius, FFI
                "MIT",              # JCodings, minitest, did_you_mean, rake
            ]
        },
    },

    "libraries": {

        # ------------- Libraries -------------

        "JONI": {
            "maven": {
                "groupId": "org.jruby.joni",
                "artifactId": "joni",
                "version": "2.1.40"
            },
            "sha1": "18766fa1b624d615d3a1cac513bf729adfd6b38f",
            "sourceSha1": "85bb9546e4c63f00f02531eeab795685566baf7b",
            "license": ["MIT"],
        },

        "JCODINGS": {
            "maven": {
                "groupId": "org.jruby.jcodings",
                "artifactId": "jcodings",
                "version": "1.0.55"
            },
            "sha1": "57169ef6964f44aef67b247cb87f053d93182488",
            "sourceSha1": "071891163f601416a5426d8f05b7d46139217ff1",
            "license": ["MIT"],
        },
    },

    "externalProjects": {
        "truffleruby-root": {
            "type": "ruby",
            "path": '.',
            "source": [
                "lib/json",
                "lib/mri",
                "lib/truffle",
            ],
            "load_path": ["src/main/ruby/truffleruby/core"],
            "test": ["spec", "test"],
            "excluded": [
                "dumps",
                "logo",
                "mxbuild",
                "profiles",
                ".ext",
                "truffleruby-gem-test-pack",
                "lib/json/java",
                "lib/ruby",
                "test/truffle/ecosystem/blog5",
                "test/truffle/ecosystem/blog6",
                "test/truffle/ecosystem/hello-world",
                "test/truffle/ecosystem/rails-app",
                "test/truffle/offline",
                "tool/docker",
                "rubyspec_temp",
            ]
        },
    },

    "projects": {

        # ------------- Projects -------------

        "org.truffleruby.annotations": {
            "dir": "src/annotations",
            "sourceDirs": ["java"],
            "javaCompliance": "8+",
            "checkstyle": "org.truffleruby",
            "workingSets": "TruffleRuby",
            "checkPackagePrefix": "false",
            "license": ["EPL-2.0"],
        },

        "org.truffleruby.shared": {
            "dir": "src/shared",
            "sourceDirs": ["java"],
            "dependencies": [
                "truffleruby:TRUFFLERUBY-ANNOTATIONS",
                "sdk:GRAAL_SDK",
            ],
            "annotationProcessors": [
                "TRUFFLERUBY-PROCESSOR",
            ],
            "javaCompliance": "8+",
            "checkstyle": "org.truffleruby",
            "workingSets": "TruffleRuby",
            "checkPackagePrefix": "false",
            "license": ["EPL-2.0"],
        },

        "org.truffleruby.processor": {
            "dir": "src/processor",
            "sourceDirs": ["java"],
            "dependencies": [
                "truffleruby:TRUFFLERUBY-ANNOTATIONS",
                "truffle:TRUFFLE_API",
            ],
            "javaCompliance": "8+",
            "checkstyle": "org.truffleruby",
            "workingSets": "TruffleRuby",
            "checkPackagePrefix": "false",
            "license": ["EPL-2.0"],
        },

        "org.truffleruby.services": {
            "dir": "src/services",
            "sourceDirs": ["java"],
            "dependencies": [
                "sdk:GRAAL_SDK",
            ],
            "javaCompliance": "8+",
            "checkstyle": "org.truffleruby",
            "workingSets": "TruffleRuby",
            "checkPackagePrefix": "false",
            "license": ["EPL-2.0"],
        },

        "org.truffleruby": {
            "dir": "src/main",
            "sourceDirs": ["java"],
            "dependencies": [
                "truffleruby:TRUFFLERUBY-ANNOTATIONS",
                "truffleruby:TRUFFLERUBY-SHARED",
                "truffle:TRUFFLE_API",
                "sdk:JLINE3",
                "sulong:SULONG_API",
                "JONI",
                "JCODINGS",
            ],
            "annotationProcessors": [
                "truffle:TRUFFLE_DSL_PROCESSOR",
                "TRUFFLERUBY-PROCESSOR",
            ],
            "javaCompliance": "8+",
            "checkstyle": "org.truffleruby",
            "workingSets": "TruffleRuby",
            "findbugsIgnoresGenerated": True,
            "checkPackagePrefix": "false",
            "license": [
                "EPL-2.0",          # JRuby (we're choosing EPL out of EPL,GPL,LGPL)
                "BSD-new",          # Rubinius
                "BSD-simplified",   # MRI
                "MIT",              # Joni, JCodings
            ],
        },

        "org.truffleruby.ruby": {
            "dir": "src/main/ruby",
            "sourceDirs": ["."],
            "javaCompliance": "8+",
            "license": [
                "EPL-2.0",          # JRuby (we're choosing EPL out of EPL,GPL,LGPL)
                "BSD-new",          # Rubinius
            ],
            "externalProjects": {
                "core-library": {
                    "type": "ruby",
                    "path": "truffleruby",
                    "source": ["core", "post-boot"],
                    "load_path": ["core"]
                }
            }
        },

        "org.truffleruby.launcher": {
            "dir": "src/launcher",
            "sourceDirs": ["java"],
            "dependencies": [
                "truffleruby:TRUFFLERUBY-ANNOTATIONS",
                "truffleruby:TRUFFLERUBY-SHARED",
                "sdk:GRAAL_SDK",
                "sdk:LAUNCHER_COMMON",
            ],
            "javaCompliance": "8+",
            "checkstyle": "org.truffleruby",
            "workingSets": "TruffleRuby",
            "checkPackagePrefix": "false",
            "license": ["EPL-2.0"],
        },

        "org.truffleruby.test": {
            "dir": "src/test",
            "sourceDirs": ["java", "ruby"],
            "dependencies": [
                "org.truffleruby",
                "org.truffleruby.services",
                "mx:JUNIT",
            ],
            "javaCompliance": "8+",
            "checkstyle": "org.truffleruby",
            "checkPackagePrefix": "false",
            "license": ["EPL-2.0"],
        },

        "org.truffleruby.tck": {
            "testProject": True,
            "dir": "src/tck",
            "sourceDirs": ["java", "ruby"],
            "dependencies": ["truffle:TRUFFLE_TCK"],
            "javaCompliance": "8+",
            "checkstyle": "org.truffleruby",
            "license": ["EPL-2.0"],
        },

        "org.truffleruby.bootstrap.launcher": {
            "class": "TruffleRubyBootstrapLauncherProject",
            "buildDependencies": [
                "TRUFFLERUBY", # We need this jar to run extconf.rb
                "TRUFFLERUBY-LAUNCHER", # We need this jar to run extconf.rb
                "sulong:SULONG", # We need this jar to find the toolchain with Toolchain#getToolPath
            ],
            "license": ["EPL-2.0"],
        },

        "org.truffleruby.cext": {
            "native": True,
            "dir": "src/main/c",
            "buildDependencies": [
                "truffle:TRUFFLE_NFI_NATIVE", # trufflenfi.h
                "sulong:SULONG_BOOTSTRAP_TOOLCHAIN", # graalvm-native-clang
                "sulong:SULONG_HOME", # polyglot.h
                "TRUFFLERUBY-BOOTSTRAP-LAUNCHER",
            ],
            "buildEnv": {
              "NFI_HEADERS_DIR": "<path:truffle:TRUFFLE_NFI_NATIVE>/include",
              "TRUFFLERUBY_BOOTSTRAP_LAUNCHER": "<path:TRUFFLERUBY-BOOTSTRAP-LAUNCHER>/miniruby",
              "GRAALVM_TOOLCHAIN_CC": "<toolchainGetToolPath:native,CC>",
            },
            "output": ".",
            "results": [
                "src/main/c/spawn-helper/spawn-helper",
                "src/main/c/truffleposix/<lib:truffleposix>",
                "src/main/c/cext/<lib:truffleruby>",
                "src/main/c/etc/<extsuffix:etc>",
                "src/main/c/nkf/<extsuffix:nkf>",
                "src/main/c/openssl/<extsuffix:openssl>",
                "src/main/c/psych/<extsuffix:psych>",
                "src/main/c/rbconfig-sizeof/<extsuffix:sizeof>",
                "src/main/c/ripper/<extsuffix:ripper>",
                "src/main/c/syslog/<extsuffix:syslog>",
                "src/main/c/zlib/<extsuffix:zlib>",
            ],
            "license": [
                "EPL-2.0",          # JRuby (we're choosing EPL out of EPL,GPL,LGPL)
                "BSD-simplified",   # MRI
            ],
        },
    },

    "distributions": {

        # ------------- Distributions -------------

        "TRUFFLERUBY-ANNOTATIONS": {
            "dependencies": [
                "org.truffleruby.annotations"
            ],
            "description": "TruffleRuby Annotations",
            "license": ["EPL-2.0"]
        },

        # Required to share code between the launcher and the rest,
        # since the rest cannot depend on the launcher and the shared code cannot be there.
        # This code is loaded twice in different classloaders, therefore any created instances should not be passed around.
        "TRUFFLERUBY-SHARED": {
            "dependencies": [
                "org.truffleruby.shared"
            ],
            "distDependencies": [
                "truffleruby:TRUFFLERUBY-ANNOTATIONS",
                "sdk:GRAAL_SDK",
            ],
            "description": "TruffleRuby Shared constants and predicates",
            "license": ["EPL-2.0"]
        },

        "TRUFFLERUBY-PROCESSOR": {
            "dependencies": [
                "org.truffleruby.processor"
            ],
            "distDependencies": [
                "truffleruby:TRUFFLERUBY-ANNOTATIONS",
                "truffle:TRUFFLE_API",
            ],
            "description": "TruffleRuby Annotation Processor",
            "license": ["EPL-2.0"],
        },

        "TRUFFLERUBY-SERVICES": {
            "moduleInfo": {
                "name": "org.truffleruby.services",
                "requires": ["java.scripting"],
                "exports": ["org.truffleruby.services.scriptengine"],
            },
            "dependencies": [
                "org.truffleruby.services"
            ],
            "distDependencies": [
                "sdk:GRAAL_SDK",
            ],
            "description": "TruffleRuby services",
            "license": ["EPL-2.0"]
        },

        "TRUFFLERUBY": {
            "mainClass": "org.truffleruby.launcher.RubyLauncher",
            "dependencies": [
                "org.truffleruby",
                "org.truffleruby.ruby",
            ],
            "distDependencies": [
                "truffle:TRUFFLE_API",
                "truffle:TRUFFLE_NFI",
                "sulong:SULONG_API",
                "truffleruby:TRUFFLERUBY-ANNOTATIONS",
                "truffleruby:TRUFFLERUBY-SHARED",
            ],
            "description": "TruffleRuby",
            "license": [
                "EPL-2.0",          # JRuby (we're choosing EPL out of EPL,GPL,LGPL)
                "BSD-new",          # Rubinius
                "BSD-simplified",   # MRI
                "MIT",              # Joni, JCodings
            ],
        },

        "TRUFFLERUBY-BOOTSTRAP-LAUNCHER": {
            "native": True,
            "layout": {
                "./": "dependency:org.truffleruby.bootstrap.launcher/*",
            },
            "description": "TruffleRuby Bootstrap Launcher to build core C extensions",
            "license": ["EPL-2.0"],
        },

        "TRUFFLERUBY-LAUNCHER": {
            "dependencies": [
                "org.truffleruby.launcher"
            ],
            "distDependencies": [
                "truffleruby:TRUFFLERUBY-ANNOTATIONS",
                "truffleruby:TRUFFLERUBY-SHARED",
                "truffleruby:TRUFFLERUBY-SERVICES",     # For the file type detection service
                "sdk:GRAAL_SDK",
                "sdk:LAUNCHER_COMMON",
            ],
            "description": "TruffleRuby Launcher",
            "license": ["EPL-2.0"],
        },

        "TRUFFLERUBY_GRAALVM_SUPPORT": {
            "native": True,
            "platformDependent": True,
            "description": "TruffleRuby support distribution for the GraalVM",
            "layout": {
                "./": [
                    "file:CHANGELOG.md",
                    "file:README.md",
                    "file:mx.truffleruby/native-image.properties",
                ],
                "bin/": [
                    "file:bin/bundle",
                    "file:bin/bundler",
                    "file:bin/gem",
                    "file:bin/irb",
                    "file:bin/rake",
                    "file:bin/rdoc",
                    "file:bin/ri",
                ],
                "doc/": [
                    "file:doc/user",
                    "file:doc/legal",
                ],
                "logo/": [
                    "file:logo/ATTRIBUTION.md",
                    "file:logo/LICENSE.txt",
                ],
                "logo/png/": [
                    "file:logo/png/truffleruby_logo_horizontal_medium.png",
                ],
                "lib/": [
                    "file:lib/json",
                    "file:lib/gems",
                    "file:lib/mri",
                    "file:lib/patches",
                    "file:lib/truffle",
                ],
                "lib/cext/": [
                    "file:lib/cext/*.rb",
                    "dependency:org.truffleruby.cext/src/main/c/truffleposix/<lib:truffleposix>",
                    "dependency:org.truffleruby.cext/src/main/c/cext/<lib:truffleruby>",
                ],
                "lib/cext/include/": [
                    "file:lib/cext/include/ccan",
                    "file:lib/cext/include/ruby",
                    "file:lib/cext/include/truffleruby",
                    "file:lib/cext/include/*.h",
                ],
                "lib/mri/": [
                    "dependency:org.truffleruby.cext/src/main/c/etc/<extsuffix:etc>",
                    "dependency:org.truffleruby.cext/src/main/c/nkf/<extsuffix:nkf>",
                    "dependency:org.truffleruby.cext/src/main/c/openssl/<extsuffix:openssl>",
                    "dependency:org.truffleruby.cext/src/main/c/psych/<extsuffix:psych>",
                    "dependency:org.truffleruby.cext/src/main/c/ripper/<extsuffix:ripper>",
                    "dependency:org.truffleruby.cext/src/main/c/syslog/<extsuffix:syslog>",
                    "dependency:org.truffleruby.cext/src/main/c/zlib/<extsuffix:zlib>",
                ],
                "lib/mri/rbconfig/": [
                    "dependency:org.truffleruby.cext/src/main/c/rbconfig-sizeof/<extsuffix:sizeof>",
                ],
                "lib/truffle/": [
                    "dependency:org.truffleruby.cext/src/main/c/spawn-helper/spawn-helper",
                ],
                "src/main/c/openssl/": [
                    "file:src/main/c/openssl/deprecation.rb",
                    "file:src/main/c/openssl/extconf.rb",
                    "file:src/main/c/openssl/*.c",
                    {
                        "source_type": "file",
                        "path": "src/main/c/openssl/*.h",
                        "exclude": ["src/main/c/openssl/extconf.h"]
                    },
                ],
            },
            "license": [
                "EPL-2.0",          # JRuby (we're choosing EPL out of EPL,GPL,LGPL)
                "MIT",              # minitest, did_you_mean, rake
                "BSD-simplified",   # MRI
                "BSD-new",          # Rubinius, FFI
            ],
        },

        "TRUFFLERUBY_GRAALVM_LICENSES": {
            "native": True,
            "platformDependent": True,
            "description": "TruffleRuby support distribution for the GraalVM license files",
            "layout": {
                "LICENSE_TRUFFLERUBY.txt": "file:LICENCE.md",
                "3rd_party_licenses_truffleruby.txt": "file:3rd_party_licenses.txt",
            },
        },

        "TRUFFLERUBY-TEST": {
            "testDistribution": True,
            "dependencies": [
                "org.truffleruby.test",
            ],
            "exclude": [
                "mx:HAMCREST",
                "mx:JUNIT"
            ],
            "distDependencies": [
                "TRUFFLERUBY",
                "TRUFFLERUBY-SERVICES",
            ],
            "license": ["EPL-2.0"],
        },

        "TRUFFLERUBY-TCK": {
            "testDistribution": True,
            "dependencies": ["org.truffleruby.tck"],
            "distDependencies": ["truffle:TRUFFLE_TCK"],
            "license": ["EPL-2.0"],
        },
    },
}
