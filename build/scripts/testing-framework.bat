@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  testing-framework startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and TESTING_FRAMEWORK_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\testing-framework-1.0-SNAPSHOT.jar;%APP_HOME%\lib\akka-actor-typed_2.12-2.5.23.jar;%APP_HOME%\lib\akka-actor_2.12-2.5.23.jar;%APP_HOME%\lib\spring-boot-starter-websocket-2.1.6.RELEASE.jar;%APP_HOME%\lib\spring-boot-starter-web-2.1.6.RELEASE.jar;%APP_HOME%\lib\spring-boot-starter-json-2.1.6.RELEASE.jar;%APP_HOME%\lib\jackson-datatype-jdk8-2.9.9.jar;%APP_HOME%\lib\jackson-datatype-jsr310-2.9.9.jar;%APP_HOME%\lib\jackson-module-parameter-names-2.9.9.jar;%APP_HOME%\lib\jackson-databind-2.9.9.jar;%APP_HOME%\lib\httpclient-4.5.9.jar;%APP_HOME%\lib\iot-device-client-1.18.0.jar;%APP_HOME%\lib\iot-service-client-1.18.0.jar;%APP_HOME%\lib\iot-deps-0.8.5.jar;%APP_HOME%\lib\org.eclipse.paho.client.mqttv3-1.2.2.jar;%APP_HOME%\lib\spring-websocket-5.1.8.RELEASE.jar;%APP_HOME%\lib\spring-messaging-5.1.8.RELEASE.jar;%APP_HOME%\lib\json-20180813.jar;%APP_HOME%\lib\hivemq-mqtt-client-shaded-1.1.0.jar;%APP_HOME%\lib\commons-text-1.6.jar;%APP_HOME%\lib\jedis-3.1.0.jar;%APP_HOME%\lib\junit-jupiter-params-5.3.2.jar;%APP_HOME%\lib\junit-jupiter-api-5.3.2.jar;%APP_HOME%\lib\commons-io-2.6.jar;%APP_HOME%\lib\zip4j-2.1.4.jar;%APP_HOME%\lib\amqp-client-5.7.3.jar;%APP_HOME%\lib\classgraph-4.8.47.jar;%APP_HOME%\lib\jaxb-impl-2.3.2.jar;%APP_HOME%\lib\jaxb-core-2.3.0.1.jar;%APP_HOME%\lib\jaxb-api-2.3.1.jar;%APP_HOME%\lib\javassist-3.24.1-GA.jar;%APP_HOME%\lib\metrics-core-4.0.5.jar;%APP_HOME%\lib\scala-java8-compat_2.12-0.8.0.jar;%APP_HOME%\lib\scala-library-2.12.8.jar;%APP_HOME%\lib\config-1.3.3.jar;%APP_HOME%\lib\jackson-annotations-2.9.0.jar;%APP_HOME%\lib\azure-storage-2.2.0.jar;%APP_HOME%\lib\jackson-core-2.9.9.jar;%APP_HOME%\lib\httpcore-4.4.11.jar;%APP_HOME%\lib\commons-logging-1.2.jar;%APP_HOME%\lib\security-provider-1.3.0.jar;%APP_HOME%\lib\commons-codec-1.11.jar;%APP_HOME%\lib\spring-webmvc-5.1.8.RELEASE.jar;%APP_HOME%\lib\spring-boot-starter-2.1.6.RELEASE.jar;%APP_HOME%\lib\spring-boot-autoconfigure-2.1.6.RELEASE.jar;%APP_HOME%\lib\spring-boot-2.1.6.RELEASE.jar;%APP_HOME%\lib\spring-context-5.1.8.RELEASE.jar;%APP_HOME%\lib\spring-web-5.1.8.RELEASE.jar;%APP_HOME%\lib\spring-aop-5.1.8.RELEASE.jar;%APP_HOME%\lib\spring-beans-5.1.8.RELEASE.jar;%APP_HOME%\lib\spring-expression-5.1.8.RELEASE.jar;%APP_HOME%\lib\spring-core-5.1.8.RELEASE.jar;%APP_HOME%\lib\rxjava-2.2.5.jar;%APP_HOME%\lib\commons-lang3-3.8.1.jar;%APP_HOME%\lib\qpid-proton-j-extensions-1.2.1.jar;%APP_HOME%\lib\spring-boot-starter-logging-2.1.6.RELEASE.jar;%APP_HOME%\lib\logback-classic-1.2.3.jar;%APP_HOME%\lib\log4j-to-slf4j-2.11.2.jar;%APP_HOME%\lib\jul-to-slf4j-1.7.26.jar;%APP_HOME%\lib\slf4j-api-1.8.0-alpha2.jar;%APP_HOME%\lib\commons-pool2-2.6.2.jar;%APP_HOME%\lib\junit-platform-commons-1.3.2.jar;%APP_HOME%\lib\apiguardian-api-1.0.0.jar;%APP_HOME%\lib\opentest4j-1.1.1.jar;%APP_HOME%\lib\jnr-unixsocket-0.23.jar;%APP_HOME%\lib\proton-j-0.31.0.jar;%APP_HOME%\lib\gson-2.8.1.jar;%APP_HOME%\lib\javax.json-1.0.4.jar;%APP_HOME%\lib\javax.activation-api-1.2.0.jar;%APP_HOME%\lib\spring-boot-starter-tomcat-2.1.6.RELEASE.jar;%APP_HOME%\lib\hibernate-validator-6.0.17.Final.jar;%APP_HOME%\lib\reactive-streams-1.0.2.jar;%APP_HOME%\lib\jnr-enxio-0.21.jar;%APP_HOME%\lib\jnr-posix-3.0.50.jar;%APP_HOME%\lib\jnr-ffi-2.1.10.jar;%APP_HOME%\lib\jnr-constants-0.9.12.jar;%APP_HOME%\lib\bcmail-jdk15on-1.61.jar;%APP_HOME%\lib\bcpkix-jdk15on-1.61.jar;%APP_HOME%\lib\bcprov-jdk15on-1.61.jar;%APP_HOME%\lib\spring-jcl-5.1.8.RELEASE.jar;%APP_HOME%\lib\javax.annotation-api-1.3.2.jar;%APP_HOME%\lib\snakeyaml-1.23.jar;%APP_HOME%\lib\tomcat-embed-websocket-9.0.21.jar;%APP_HOME%\lib\tomcat-embed-core-9.0.21.jar;%APP_HOME%\lib\tomcat-embed-el-9.0.21.jar;%APP_HOME%\lib\validation-api-2.0.1.Final.jar;%APP_HOME%\lib\jboss-logging-3.3.2.Final.jar;%APP_HOME%\lib\classmate-1.3.4.jar;%APP_HOME%\lib\jffi-1.2.19.jar;%APP_HOME%\lib\jffi-1.2.19-native.jar;%APP_HOME%\lib\asm-commons-7.1.jar;%APP_HOME%\lib\asm-util-7.1.jar;%APP_HOME%\lib\asm-analysis-7.1.jar;%APP_HOME%\lib\asm-tree-7.1.jar;%APP_HOME%\lib\asm-7.1.jar;%APP_HOME%\lib\jnr-a64asm-1.0.0.jar;%APP_HOME%\lib\jnr-x86asm-1.0.2.jar;%APP_HOME%\lib\logback-core-1.2.3.jar;%APP_HOME%\lib\log4j-api-2.11.2.jar;%APP_HOME%\lib\tomcat-annotations-api-9.0.21.jar

@rem Execute testing-framework
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %TESTING_FRAMEWORK_OPTS%  -classpath "%CLASSPATH%" com.ds.iot.framework.examples.Main %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable TESTING_FRAMEWORK_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%TESTING_FRAMEWORK_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
