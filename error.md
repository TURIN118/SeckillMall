D:\SOFTWARE\Java\JDK17\bin\java.exe -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:58532,suspend=y,server=n -javaagent:C:\Users\WNJ\AppData\Local\JetBrains\IntelliJIdea2025.3\captureAgent\debugger-agent.jar=file:///C:/Users/WNJ/AppData/Local/Temp/capture1685529400598415255.props -agentpath:C:\Users\WNJ\AppData\Local\Temp\idea_libasyncProfiler_dll_temp_folder\libasyncProfiler.dll=version,jfr,event=wall,interval=10ms,cstack=no,file=C:\Users\WNJ\IdeaSnapshots\SeckillMallApplication_2026_08_04_133804.jfr,dbghelppath=C:\Users\WNJ\AppData\Local\Temp\idea_dbghelp_dll_temp_folder\dbghelp.dll,log=C:\Users\WNJ\AppData\Local\Temp\SeckillMallApplication_2026_08_04_133804.jfr.log.txt,logLevel=DEBUG -XX:TieredStopAtLevel=1 -Dspring.output.ansi.enabled=always -Dcom.sun.management.jmxremote -Dspring.jmx.enabled=true -Dspring.liveBeansView.mbeanDomain -Dspring.application.admin.enabled=true "-Dmanagement.endpoints.jmx.exposure.include=*" -Dkotlinx.coroutines.debug.enable.creation.stack.trace=false -Ddebugger.agent.enable.coroutines=true -Dkotlinx.coroutines.debug.enable.flows.stack.trace=true -Dkotlinx.coroutines.debug.enable.mutable.state.flows.stack.trace=true -Ddebugger.async.stack.trace.for.all.threads=true -Dfile.encoding=UTF-8 -classpath "D:\DESk\SpringBoot\seckill-mall\target\classes;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot-starter-web\3.2.5\spring-boot-starter-web-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot-starter\3.2.5\spring-boot-starter-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot\3.2.5\spring-boot-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot-starter-logging\3.2.5\spring-boot-starter-logging-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\ch\qos\logback\logback-classic\1.4.14\logback-classic-1.4.14.jar;D:\SOFTWARE\Maven\maven-repository\ch\qos\logback\logback-core\1.4.14\logback-core-1.4.14.jar;D:\SOFTWARE\Maven\maven-repository\org\apache\logging\log4j\log4j-to-slf4j\2.21.1\log4j-to-slf4j-2.21.1.jar;D:\SOFTWARE\Maven\maven-repository\org\slf4j\jul-to-slf4j\2.0.13\jul-to-slf4j-2.0.13.jar;D:\SOFTWARE\Maven\maven-repository\jakarta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;D:\SOFTWARE\Maven\maven-repository\org\yaml\snakeyaml\2.2\snakeyaml-2.2.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot-starter-json\3.2.5\spring-boot-starter-json-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\com\fasterxml\jackson\datatype\jackson-datatype-jdk8\2.15.4\jackson-datatype-jdk8-2.15.4.jar;D:\SOFTWARE\Maven\maven-repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.15.4\jackson-datatype-jsr310-2.15.4.jar;D:\SOFTWARE\Maven\maven-repository\com\fasterxml\jackson\module\jackson-module-parameter-names\2.15.4\jackson-module-parameter-names-2.15.4.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot-starter-tomcat\3.2.5\spring-boot-starter-tomcat-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.20\tomcat-embed-core-10.1.20.jar;D:\SOFTWARE\Maven\maven-repository\org\apache\tomcat\embed\tomcat-embed-websocket\10.1.20\tomcat-embed-websocket-10.1.20.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\spring-web\6.1.6\spring-web-6.1.6.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\spring-beans\6.1.6\spring-beans-6.1.6.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\spring-webmvc\6.1.6\spring-webmvc-6.1.6.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\spring-context\6.1.6\spring-context-6.1.6.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\spring-expression\6.1.6\spring-expression-6.1.6.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot-starter-security\3.2.5\spring-boot-starter-security-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\spring-aop\6.1.6\spring-aop-6.1.6.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\security\spring-security-config\6.2.4\spring-security-config-6.2.4.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\security\spring-security-web\6.2.4\spring-security-web-6.2.4.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot-starter-data-redis\3.2.5\spring-boot-starter-data-redis-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\io\lettuce\lettuce-core\6.3.2.RELEASE\lettuce-core-6.3.2.RELEASE.jar;D:\SOFTWARE\Maven\maven-repository\io\netty\netty-common\4.1.109.Final\netty-common-4.1.109.Final.jar;D:\SOFTWARE\Maven\maven-repository\io\netty\netty-handler\4.1.109.Final\netty-handler-4.1.109.Final.jar;D:\SOFTWARE\Maven\maven-repository\io\netty\netty-transport-native-unix-common\4.1.109.Final\netty-transport-native-unix-common-4.1.109.Final.jar;D:\SOFTWARE\Maven\maven-repository\io\netty\netty-transport\4.1.109.Final\netty-transport-4.1.109.Final.jar;D:\SOFTWARE\Maven\maven-repository\io\projectreactor\reactor-core\3.6.5\reactor-core-3.6.5.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\data\spring-data-redis\3.2.5\spring-data-redis-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\data\spring-data-keyvalue\3.2.5\spring-data-keyvalue-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\data\spring-data-commons\3.2.5\spring-data-commons-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\spring-tx\6.1.6\spring-tx-6.1.6.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\spring-oxm\6.1.6\spring-oxm-6.1.6.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot-starter-validation\3.2.5\spring-boot-starter-validation-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\org\apache\tomcat\embed\tomcat-embed-el\10.1.20\tomcat-embed-el-10.1.20.jar;D:\SOFTWARE\Maven\maven-repository\org\hibernate\validator\hibernate-validator\8.0.1.Final\hibernate-validator-8.0.1.Final.jar;D:\SOFTWARE\Maven\maven-repository\jakarta\validation\jakarta.validation-api\3.0.2\jakarta.validation-api-3.0.2.jar;D:\SOFTWARE\Maven\maven-repository\org\jboss\logging\jboss-logging\3.5.3.Final\jboss-logging-3.5.3.Final.jar;D:\SOFTWARE\Maven\maven-repository\com\fasterxml\classmate\1.6.0\classmate-1.6.0.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot-starter-amqp\3.2.5\spring-boot-starter-amqp-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\spring-messaging\6.1.6\spring-messaging-6.1.6.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\amqp\spring-rabbit\3.1.4\spring-rabbit-3.1.4.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\amqp\spring-amqp\3.1.4\spring-amqp-3.1.4.jar;D:\SOFTWARE\Maven\maven-repository\com\rabbitmq\amqp-client\5.19.0\amqp-client-5.19.0.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot-starter-mail\3.2.5\spring-boot-starter-mail-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\spring-context-support\6.1.6\spring-context-support-6.1.6.jar;D:\SOFTWARE\Maven\maven-repository\org\eclipse\angus\jakarta.mail\2.0.3\jakarta.mail-2.0.3.jar;D:\SOFTWARE\Maven\maven-repository\jakarta\activation\jakarta.activation-api\2.1.3\jakarta.activation-api-2.1.3.jar;D:\SOFTWARE\Maven\maven-repository\org\eclipse\angus\angus-activation\2.0.2\angus-activation-2.0.2.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot-starter-thymeleaf\3.2.5\spring-boot-starter-thymeleaf-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\org\thymeleaf\thymeleaf-spring6\3.1.2.RELEASE\thymeleaf-spring6-3.1.2.RELEASE.jar;D:\SOFTWARE\Maven\maven-repository\org\thymeleaf\thymeleaf\3.1.2.RELEASE\thymeleaf-3.1.2.RELEASE.jar;D:\SOFTWARE\Maven\maven-repository\org\attoparser\attoparser\2.0.7.RELEASE\attoparser-2.0.7.RELEASE.jar;D:\SOFTWARE\Maven\maven-repository\org\unbescape\unbescape\1.1.6.RELEASE\unbescape-1.1.6.RELEASE.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot-starter-actuator\3.2.5\spring-boot-starter-actuator-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot-actuator-autoconfigure\3.2.5\spring-boot-actuator-autoconfigure-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot-actuator\3.2.5\spring-boot-actuator-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\io\micrometer\micrometer-observation\1.12.5\micrometer-observation-1.12.5.jar;D:\SOFTWARE\Maven\maven-repository\io\micrometer\micrometer-commons\1.12.5\micrometer-commons-1.12.5.jar;D:\SOFTWARE\Maven\maven-repository\io\micrometer\micrometer-jakarta9\1.12.5\micrometer-jakarta9-1.12.5.jar;D:\SOFTWARE\Maven\maven-repository\io\micrometer\micrometer-registry-prometheus\1.12.5\micrometer-registry-prometheus-1.12.5.jar;D:\SOFTWARE\Maven\maven-repository\io\micrometer\micrometer-core\1.12.5\micrometer-core-1.12.5.jar;D:\SOFTWARE\Maven\maven-repository\org\hdrhistogram\HdrHistogram\2.1.12\HdrHistogram-2.1.12.jar;D:\SOFTWARE\Maven\maven-repository\org\latencyutils\LatencyUtils\2.0.3\LatencyUtils-2.0.3.jar;D:\SOFTWARE\Maven\maven-repository\io\prometheus\simpleclient_common\0.16.0\simpleclient_common-0.16.0.jar;D:\SOFTWARE\Maven\maven-repository\io\prometheus\simpleclient\0.16.0\simpleclient-0.16.0.jar;D:\SOFTWARE\Maven\maven-repository\io\prometheus\simpleclient_tracer_otel\0.16.0\simpleclient_tracer_otel-0.16.0.jar;D:\SOFTWARE\Maven\maven-repository\io\prometheus\simpleclient_tracer_common\0.16.0\simpleclient_tracer_common-0.16.0.jar;D:\SOFTWARE\Maven\maven-repository\io\prometheus\simpleclient_tracer_otel_agent\0.16.0\simpleclient_tracer_otel_agent-0.16.0.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\retry\spring-retry\2.0.5\spring-retry-2.0.5.jar;D:\SOFTWARE\Maven\maven-repository\org\aspectj\aspectjweaver\1.9.22\aspectjweaver-1.9.22.jar;D:\SOFTWARE\Maven\maven-repository\com\baomidou\mybatis-plus-spring-boot3-starter\3.5.5\mybatis-plus-spring-boot3-starter-3.5.5.jar;D:\SOFTWARE\Maven\maven-repository\com\baomidou\mybatis-plus\3.5.5\mybatis-plus-3.5.5.jar;D:\SOFTWARE\Maven\maven-repository\com\baomidou\mybatis-plus-core\3.5.5\mybatis-plus-core-3.5.5.jar;D:\SOFTWARE\Maven\maven-repository\com\baomidou\mybatis-plus-annotation\3.5.5\mybatis-plus-annotation-3.5.5.jar;D:\SOFTWARE\Maven\maven-repository\com\baomidou\mybatis-plus-extension\3.5.5\mybatis-plus-extension-3.5.5.jar;D:\SOFTWARE\Maven\maven-repository\org\mybatis\mybatis\3.5.15\mybatis-3.5.15.jar;D:\SOFTWARE\Maven\maven-repository\com\github\jsqlparser\jsqlparser\4.6\jsqlparser-4.6.jar;D:\SOFTWARE\Maven\maven-repository\org\mybatis\mybatis-spring\3.0.3\mybatis-spring-3.0.3.jar;D:\SOFTWARE\Maven\maven-repository\com\baomidou\mybatis-plus-spring-boot-autoconfigure\3.5.5\mybatis-plus-spring-boot-autoconfigure-3.5.5.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot-autoconfigure\3.2.5\spring-boot-autoconfigure-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\boot\spring-boot-starter-jdbc\3.2.5\spring-boot-starter-jdbc-3.2.5.jar;D:\SOFTWARE\Maven\maven-repository\com\zaxxer\HikariCP\5.0.1\HikariCP-5.0.1.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\spring-jdbc\6.1.6\spring-jdbc-6.1.6.jar;D:\SOFTWARE\Maven\maven-repository\org\redisson\redisson-spring-boot-starter\3.27.2\redisson-spring-boot-starter-3.27.2.jar;D:\SOFTWARE\Maven\maven-repository\org\redisson\redisson\3.27.2\redisson-3.27.2.jar;D:\SOFTWARE\Maven\maven-repository\io\netty\netty-codec\4.1.109.Final\netty-codec-4.1.109.Final.jar;D:\SOFTWARE\Maven\maven-repository\io\netty\netty-buffer\4.1.109.Final\netty-buffer-4.1.109.Final.jar;D:\SOFTWARE\Maven\maven-repository\io\netty\netty-resolver\4.1.109.Final\netty-resolver-4.1.109.Final.jar;D:\SOFTWARE\Maven\maven-repository\io\netty\netty-resolver-dns\4.1.109.Final\netty-resolver-dns-4.1.109.Final.jar;D:\SOFTWARE\Maven\maven-repository\io\netty\netty-codec-dns\4.1.109.Final\netty-codec-dns-4.1.109.Final.jar;D:\SOFTWARE\Maven\maven-repository\javax\cache\cache-api\1.1.1\cache-api-1.1.1.jar;D:\SOFTWARE\Maven\maven-repository\org\reactivestreams\reactive-streams\1.0.4\reactive-streams-1.0.4.jar;D:\SOFTWARE\Maven\maven-repository\io\reactivex\rxjava3\rxjava\3.1.8\rxjava-3.1.8.jar;D:\SOFTWARE\Maven\maven-repository\org\jboss\marshalling\jboss-marshalling\2.0.11.Final\jboss-marshalling-2.0.11.Final.jar;D:\SOFTWARE\Maven\maven-repository\org\jboss\marshalling\jboss-marshalling-river\2.0.11.Final\jboss-marshalling-river-2.0.11.Final.jar;D:\SOFTWARE\Maven\maven-repository\com\esotericsoftware\kryo\5.6.0\kryo-5.6.0.jar;D:\SOFTWARE\Maven\maven-repository\com\esotericsoftware\reflectasm\1.11.9\reflectasm-1.11.9.jar;D:\SOFTWARE\Maven\maven-repository\com\esotericsoftware\minlog\1.3.1\minlog-1.3.1.jar;D:\SOFTWARE\Maven\maven-repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.15.4\jackson-dataformat-yaml-2.15.4.jar;D:\SOFTWARE\Maven\maven-repository\net\bytebuddy\byte-buddy\1.14.13\byte-buddy-1.14.13.jar;D:\SOFTWARE\Maven\maven-repository\org\jodd\jodd-bean\5.1.6\jodd-bean-5.1.6.jar;D:\SOFTWARE\Maven\maven-repository\org\jodd\jodd-core\5.1.6\jodd-core-5.1.6.jar;D:\SOFTWARE\Maven\maven-repository\org\redisson\redisson-spring-data-32\3.27.2\redisson-spring-data-32-3.27.2.jar;D:\SOFTWARE\Maven\maven-repository\com\github\xiaoymin\knife4j-openapi3-jakarta-spring-boot-starter\4.5.0\knife4j-openapi3-jakarta-spring-boot-starter-4.5.0.jar;D:\SOFTWARE\Maven\maven-repository\com\github\xiaoymin\knife4j-core\4.5.0\knife4j-core-4.5.0.jar;D:\SOFTWARE\Maven\maven-repository\com\github\xiaoymin\knife4j-openapi3-ui\4.5.0\knife4j-openapi3-ui-4.5.0.jar;D:\SOFTWARE\Maven\maven-repository\org\springdoc\springdoc-openapi-starter-webmvc-ui\2.3.0\springdoc-openapi-starter-webmvc-ui-2.3.0.jar;D:\SOFTWARE\Maven\maven-repository\org\springdoc\springdoc-openapi-starter-webmvc-api\2.3.0\springdoc-openapi-starter-webmvc-api-2.3.0.jar;D:\SOFTWARE\Maven\maven-repository\org\springdoc\springdoc-openapi-starter-common\2.3.0\springdoc-openapi-starter-common-2.3.0.jar;D:\SOFTWARE\Maven\maven-repository\io\swagger\core\v3\swagger-core-jakarta\2.2.19\swagger-core-jakarta-2.2.19.jar;D:\SOFTWARE\Maven\maven-repository\org\apache\commons\commons-lang3\3.13.0\commons-lang3-3.13.0.jar;D:\SOFTWARE\Maven\maven-repository\io\swagger\core\v3\swagger-annotations-jakarta\2.2.19\swagger-annotations-jakarta-2.2.19.jar;D:\SOFTWARE\Maven\maven-repository\io\swagger\core\v3\swagger-models-jakarta\2.2.19\swagger-models-jakarta-2.2.19.jar;D:\SOFTWARE\Maven\maven-repository\org\webjars\swagger-ui\5.10.3\swagger-ui-5.10.3.jar;D:\SOFTWARE\Maven\maven-repository\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar;D:\SOFTWARE\Maven\maven-repository\org\projectlombok\lombok\1.18.30\lombok-1.18.30.jar;D:\SOFTWARE\Maven\maven-repository\org\mapstruct\mapstruct\1.5.5.Final\mapstruct-1.5.5.Final.jar;D:\SOFTWARE\Maven\maven-repository\org\jsoup\jsoup\1.17.2\jsoup-1.17.2.jar;D:\SOFTWARE\Maven\maven-repository\io\minio\minio\8.5.7\minio-8.5.7.jar;D:\SOFTWARE\Maven\maven-repository\com\carrotsearch\thirdparty\simple-xml-safe\2.7.1\simple-xml-safe-2.7.1.jar;D:\SOFTWARE\Maven\maven-repository\com\google\guava\guava\32.1.3-jre\guava-32.1.3-jre.jar;D:\SOFTWARE\Maven\maven-repository\com\google\guava\failureaccess\1.0.1\failureaccess-1.0.1.jar;D:\SOFTWARE\Maven\maven-repository\com\google\guava\listenablefuture\9999.0-empty-to-avoid-conflict-with-guava\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;D:\SOFTWARE\Maven\maven-repository\com\google\code\findbugs\jsr305\3.0.2\jsr305-3.0.2.jar;D:\SOFTWARE\Maven\maven-repository\org\checkerframework\checker-qual\3.37.0\checker-qual-3.37.0.jar;D:\SOFTWARE\Maven\maven-repository\com\google\errorprone\error_prone_annotations\2.21.1\error_prone_annotations-2.21.1.jar;D:\SOFTWARE\Maven\maven-repository\com\google\j2objc\j2objc-annotations\2.8\j2objc-annotations-2.8.jar;D:\SOFTWARE\Maven\maven-repository\com\squareup\okhttp3\okhttp\4.12.0\okhttp-4.12.0.jar;D:\SOFTWARE\Maven\maven-repository\com\squareup\okio\okio\3.6.0\okio-3.6.0.jar;D:\SOFTWARE\Maven\maven-repository\com\squareup\okio\okio-jvm\3.6.0\okio-jvm-3.6.0.jar;D:\SOFTWARE\Maven\maven-repository\org\jetbrains\kotlin\kotlin-stdlib-common\1.9.23\kotlin-stdlib-common-1.9.23.jar;D:\SOFTWARE\Maven\maven-repository\org\jetbrains\kotlin\kotlin-stdlib-jdk8\1.9.23\kotlin-stdlib-jdk8-1.9.23.jar;D:\SOFTWARE\Maven\maven-repository\org\jetbrains\kotlin\kotlin-stdlib\1.9.23\kotlin-stdlib-1.9.23.jar;D:\SOFTWARE\Maven\maven-repository\org\jetbrains\kotlin\kotlin-stdlib-jdk7\1.9.23\kotlin-stdlib-jdk7-1.9.23.jar;D:\SOFTWARE\Maven\maven-repository\com\fasterxml\jackson\core\jackson-annotations\2.15.4\jackson-annotations-2.15.4.jar;D:\SOFTWARE\Maven\maven-repository\com\fasterxml\jackson\core\jackson-core\2.15.4\jackson-core-2.15.4.jar;D:\SOFTWARE\Maven\maven-repository\com\fasterxml\jackson\core\jackson-databind\2.15.4\jackson-databind-2.15.4.jar;D:\SOFTWARE\Maven\maven-repository\org\bouncycastle\bcprov-jdk18on\1.76\bcprov-jdk18on-1.76.jar;D:\SOFTWARE\Maven\maven-repository\org\apache\commons\commons-compress\1.24.0\commons-compress-1.24.0.jar;D:\SOFTWARE\Maven\maven-repository\org\xerial\snappy\snappy-java\1.1.10.5\snappy-java-1.1.10.5.jar;D:\SOFTWARE\Maven\maven-repository\org\apache\poi\poi-ooxml\5.2.3\poi-ooxml-5.2.3.jar;D:\SOFTWARE\Maven\maven-repository\org\apache\poi\poi\5.2.3\poi-5.2.3.jar;D:\SOFTWARE\Maven\maven-repository\commons-codec\commons-codec\1.16.1\commons-codec-1.16.1.jar;D:\SOFTWARE\Maven\maven-repository\org\apache\commons\commons-math3\3.6.1\commons-math3-3.6.1.jar;D:\SOFTWARE\Maven\maven-repository\com\zaxxer\SparseBitSet\1.2\SparseBitSet-1.2.jar;D:\SOFTWARE\Maven\maven-repository\org\apache\poi\poi-ooxml-lite\5.2.3\poi-ooxml-lite-5.2.3.jar;D:\SOFTWARE\Maven\maven-repository\org\apache\xmlbeans\xmlbeans\5.1.1\xmlbeans-5.1.1.jar;D:\SOFTWARE\Maven\maven-repository\commons-io\commons-io\2.11.0\commons-io-2.11.0.jar;D:\SOFTWARE\Maven\maven-repository\com\github\virtuald\curvesapi\1.07\curvesapi-1.07.jar;D:\SOFTWARE\Maven\maven-repository\org\apache\logging\log4j\log4j-api\2.21.1\log4j-api-2.21.1.jar;D:\SOFTWARE\Maven\maven-repository\org\apache\commons\commons-collections4\4.4\commons-collections4-4.4.jar;D:\SOFTWARE\Maven\maven-repository\io\jsonwebtoken\jjwt-api\0.12.3\jjwt-api-0.12.3.jar;D:\SOFTWARE\Maven\maven-repository\io\jsonwebtoken\jjwt-impl\0.12.3\jjwt-impl-0.12.3.jar;D:\SOFTWARE\Maven\maven-repository\io\jsonwebtoken\jjwt-jackson\0.12.3\jjwt-jackson-0.12.3.jar;D:\SOFTWARE\Maven\maven-repository\jakarta\xml\bind\jakarta.xml.bind-api\4.0.2\jakarta.xml.bind-api-4.0.2.jar;D:\SOFTWARE\Maven\maven-repository\org\objenesis\objenesis\3.3\objenesis-3.3.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\spring-core\6.1.6\spring-core-6.1.6.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\spring-jcl\6.1.6\spring-jcl-6.1.6.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\security\spring-security-core\6.2.4\spring-security-core-6.2.4.jar;D:\SOFTWARE\Maven\maven-repository\org\springframework\security\spring-security-crypto\6.2.4\spring-security-crypto-6.2.4.jar;D:\SOFTWARE\Maven\maven-repository\org\slf4j\slf4j-api\2.0.13\slf4j-api-2.0.13.jar;D:\SOFTWARE\Maven\maven-repository\org\jetbrains\annotations\17.0.0\annotations-17.0.0.jar;D:\SOFTWARE\JetBrains\IntelliJ IDEA 2025.3.4\lib\idea_rt.jar" com.seckill.mall.SeckillMallApplication
已连接到地址为 ''127.0.0.1:58532'，传输: '套接字'' 的目标虚拟机

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

2026-08-04T13:38:05.591+08:00  INFO 41488 --- [           main] c.seckill.mall.SeckillMallApplication    : Starting SeckillMallApplication using Java 17.0.8 with PID 41488 (D:\DESk\SpringBoot\seckill-mall\target\classes started by WNJ in D:\DESk\SpringBoot\seckill-mall)
2026-08-04T13:38:05.593+08:00 DEBUG 41488 --- [           main] c.seckill.mall.SeckillMallApplication    : Running with Spring Boot v3.2.5, Spring v6.1.6
2026-08-04T13:38:05.594+08:00  INFO 41488 --- [           main] c.seckill.mall.SeckillMallApplication    : The following 1 profile is active: "dev"
2026-08-04T13:38:06.579+08:00  INFO 41488 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Multiple Spring Data modules found, entering strict repository configuration mode
2026-08-04T13:38:06.581+08:00  INFO 41488 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data Redis repositories in DEFAULT mode.
2026-08-04T13:38:06.606+08:00  INFO 41488 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 15 ms. Found 0 Redis repository interfaces.
2026-08-04T13:38:07.423+08:00  INFO 41488 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8080 (http)
2026-08-04T13:38:07.432+08:00  INFO 41488 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2026-08-04T13:38:07.432+08:00  INFO 41488 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.20]
2026-08-04T13:38:07.503+08:00  INFO 41488 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2026-08-04T13:38:07.503+08:00  INFO 41488 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 1878 ms
2026-08-04T13:38:07.737+08:00  INFO 41488 --- [           main] org.redisson.Version                     : Redisson 3.27.2
2026-08-04T13:38:08.044+08:00  INFO 41488 --- [isson-netty-1-5] o.redisson.connection.ConnectionsHolder  : 1 connections initialized for localhost/127.0.0.1:6379
2026-08-04T13:38:08.117+08:00  INFO 41488 --- [sson-netty-1-20] o.redisson.connection.ConnectionsHolder  : 24 connections initialized for localhost/127.0.0.1:6379
 _ _   |_  _ _|_. ___ _ |    _ 
| | |\/|_)(_| | |_\  |_)||_|_\ 
     /               |         
                        3.5.5 
2026-08-04T13:38:08.682+08:00 ERROR 41488 --- [           main] o.s.b.web.embedded.tomcat.TomcatStarter  : Error starting Tomcat context. Exception: org.springframework.beans.factory.BeanCreationException. Message: Error creating bean with name 'replayProtectionFilter': Invocation of init method failed
2026-08-04T13:38:08.705+08:00  INFO 41488 --- [           main] o.apache.catalina.core.StandardService   : Stopping service [Tomcat]
2026-08-04T13:38:08.708+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [Thread-1] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.net.dns.ResolverConfigurationImpl.notifyAddrChange0(Native Method)
 java.base@17.0.8/sun.net.dns.ResolverConfigurationImpl$AddressChangeListener.run(ResolverConfigurationImpl.java:176)
2026-08-04T13:38:08.708+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-1] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:141)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:62)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:883)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.709+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-2] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.709+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-3] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.709+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-4] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.709+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-timer-4-1] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/java.lang.Thread.sleep(Native Method)
 app//io.netty.util.HashedWheelTimer$Worker.waitForNextTick(HashedWheelTimer.java:591)
 app//io.netty.util.HashedWheelTimer$Worker.run(HashedWheelTimer.java:487)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.710+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-5] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.710+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-6] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.710+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-7] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.710+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-3-1] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/jdk.internal.misc.Unsafe.park(Native Method)
 java.base@17.0.8/java.util.concurrent.locks.LockSupport.park(LockSupport.java:341)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:506)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3465)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3436)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1623)
 java.base@17.0.8/java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:435)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1062)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1122)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.711+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-8] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.711+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-9] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.711+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-3-2] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/jdk.internal.misc.Unsafe.park(Native Method)
 java.base@17.0.8/java.util.concurrent.locks.LockSupport.park(LockSupport.java:341)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:506)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3465)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3436)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1623)
 java.base@17.0.8/java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:435)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1062)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1122)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.711+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-10] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.712+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-11] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.713+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-3-3] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/jdk.internal.misc.Unsafe.park(Native Method)
 java.base@17.0.8/java.util.concurrent.locks.LockSupport.park(LockSupport.java:341)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:506)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3465)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3436)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1623)
 java.base@17.0.8/java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:435)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1062)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1122)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.713+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-12] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.713+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-13] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.713+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-3-4] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/jdk.internal.misc.Unsafe.park(Native Method)
 java.base@17.0.8/java.util.concurrent.locks.LockSupport.park(LockSupport.java:341)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:506)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3465)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3436)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1623)
 java.base@17.0.8/java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:435)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1062)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1122)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.713+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-14] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.714+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-15] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.714+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-3-5] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/jdk.internal.misc.Unsafe.park(Native Method)
 java.base@17.0.8/java.util.concurrent.locks.LockSupport.park(LockSupport.java:341)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:506)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3465)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3436)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1623)
 java.base@17.0.8/java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:435)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1062)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1122)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.715+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-16] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.715+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-17] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.715+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-3-6] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/jdk.internal.misc.Unsafe.park(Native Method)
 java.base@17.0.8/java.util.concurrent.locks.LockSupport.park(LockSupport.java:341)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:506)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3465)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3436)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1623)
 java.base@17.0.8/java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:435)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1062)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1122)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.715+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-18] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.716+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-19] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.716+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-3-7] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/jdk.internal.misc.Unsafe.park(Native Method)
 java.base@17.0.8/java.util.concurrent.locks.LockSupport.park(LockSupport.java:341)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:506)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3465)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3436)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1623)
 java.base@17.0.8/java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:435)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1062)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1122)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.716+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-20] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.716+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-21] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.716+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-3-8] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/jdk.internal.misc.Unsafe.park(Native Method)
 java.base@17.0.8/java.util.concurrent.locks.LockSupport.park(LockSupport.java:341)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:506)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3465)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3436)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1623)
 java.base@17.0.8/java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:435)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1062)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1122)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.716+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-22] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.716+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-23] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.718+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-3-9] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/jdk.internal.misc.Unsafe.park(Native Method)
 java.base@17.0.8/java.util.concurrent.locks.LockSupport.park(LockSupport.java:341)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:506)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3465)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3436)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1623)
 java.base@17.0.8/java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:435)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1062)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1122)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.718+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-24] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.718+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-25] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.718+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-3-10] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/jdk.internal.misc.Unsafe.park(Native Method)
 java.base@17.0.8/java.util.concurrent.locks.LockSupport.park(LockSupport.java:341)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:506)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3465)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3436)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1623)
 java.base@17.0.8/java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:435)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1062)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1122)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.718+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-26] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.719+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-27] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.719+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-3-11] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/jdk.internal.misc.Unsafe.park(Native Method)
 java.base@17.0.8/java.util.concurrent.locks.LockSupport.park(LockSupport.java:341)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:506)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3465)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3436)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1623)
 java.base@17.0.8/java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:435)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1062)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1122)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.719+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-28] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.719+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-29] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.719+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-3-12] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/jdk.internal.misc.Unsafe.park(Native Method)
 java.base@17.0.8/java.util.concurrent.locks.LockSupport.park(LockSupport.java:341)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:506)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3465)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3436)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1623)
 java.base@17.0.8/java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:435)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1062)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1122)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.720+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-30] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.720+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-31] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.720+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-3-13] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/jdk.internal.misc.Unsafe.park(Native Method)
 java.base@17.0.8/java.util.concurrent.locks.LockSupport.park(LockSupport.java:341)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:506)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3465)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3436)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1623)
 java.base@17.0.8/java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:435)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1062)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1122)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.720+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-netty-1-32] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/sun.nio.ch.WEPoll.wait(Native Method)
 java.base@17.0.8/sun.nio.ch.WEPollSelectorImpl.doSelect(WEPollSelectorImpl.java:111)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:129)
 java.base@17.0.8/sun.nio.ch.SelectorImpl.select(SelectorImpl.java:146)
 app//io.netty.channel.nio.SelectedSelectionKeySetSelector.select(SelectedSelectionKeySetSelector.java:68)
 app//io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:879)
 app//io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:526)
 app//io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
 app//io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.720+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-3-14] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/jdk.internal.misc.Unsafe.park(Native Method)
 java.base@17.0.8/java.util.concurrent.locks.LockSupport.park(LockSupport.java:341)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:506)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3465)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3436)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1623)
 java.base@17.0.8/java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:435)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1062)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1122)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.721+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-3-15] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/jdk.internal.misc.Unsafe.park(Native Method)
 java.base@17.0.8/java.util.concurrent.locks.LockSupport.park(LockSupport.java:341)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:506)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3465)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3436)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1623)
 java.base@17.0.8/java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:435)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1062)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1122)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.721+08:00  WARN 41488 --- [           main] o.a.c.loader.WebappClassLoaderBase       : The web application [ROOT] appears to have started a thread named [redisson-3-16] but has failed to stop it. This is very likely to create a memory leak. Stack trace of thread:
 java.base@17.0.8/jdk.internal.misc.Unsafe.park(Native Method)
 java.base@17.0.8/java.util.concurrent.locks.LockSupport.park(LockSupport.java:341)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:506)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.unmanagedBlock(ForkJoinPool.java:3465)
 java.base@17.0.8/java.util.concurrent.ForkJoinPool.managedBlock(ForkJoinPool.java:3436)
 java.base@17.0.8/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1623)
 java.base@17.0.8/java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:435)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1062)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1122)
 java.base@17.0.8/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 app//io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
 java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
2026-08-04T13:38:08.723+08:00  WARN 41488 --- [           main] ConfigServletWebServerApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.context.ApplicationContextException: Unable to start web server
2026-08-04T13:38:08.745+08:00  INFO 41488 --- [           main] .s.b.a.l.ConditionEvaluationReportLogger : 

Error starting ApplicationContext. To display the condition evaluation report re-run your application with 'debug' enabled.
2026-08-04T13:38:08.757+08:00 ERROR 41488 --- [           main] o.s.boot.SpringApplication               : Application run failed

org.springframework.context.ApplicationContextException: Unable to start web server
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.onRefresh(ServletWebServerApplicationContext.java:165) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:618) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:334) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1354) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1343) ~[spring-boot-3.2.5.jar:3.2.5]
	at com.seckill.mall.SeckillMallApplication.main(SeckillMallApplication.java:18) ~[classes/:na]
Caused by: org.springframework.boot.web.server.WebServerException: Unable to start embedded Tomcat
	at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.initialize(TomcatWebServer.java:145) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.<init>(TomcatWebServer.java:105) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory.getTomcatWebServer(TomcatServletWebServerFactory.java:499) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory.getWebServer(TomcatServletWebServerFactory.java:218) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.createWebServer(ServletWebServerApplicationContext.java:188) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.onRefresh(ServletWebServerApplicationContext.java:162) ~[spring-boot-3.2.5.jar:3.2.5]
	... 8 common frames omitted
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'replayProtectionFilter': Invocation of init method failed
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:222) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization(AbstractAutowireCapableBeanFactory.java:422) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1778) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:600) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:205) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.getOrderedBeansOfType(ServletContextInitializerBeans.java:210) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAsRegistrationBean(ServletContextInitializerBeans.java:173) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAsRegistrationBean(ServletContextInitializerBeans.java:168) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAdaptableBeans(ServletContextInitializerBeans.java:153) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.ServletContextInitializerBeans.<init>(ServletContextInitializerBeans.java:86) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.getServletContextInitializerBeans(ServletWebServerApplicationContext.java:266) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.selfInitialize(ServletWebServerApplicationContext.java:240) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.web.embedded.tomcat.TomcatStarter.onStartup(TomcatStarter.java:52) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.apache.catalina.core.StandardContext.startInternal(StandardContext.java:4880) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1332) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1322) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at java.base/java.util.concurrent.FutureTask.run$$$capture(FutureTask.java:264) ~[na:na]
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java) ~[na:na]
	at org.apache.tomcat.util.threads.InlineExecutorService.execute(InlineExecutorService.java:75) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at java.base/java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:145) ~[na:na]
	at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:866) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.StandardHost.startInternal(StandardHost.java:845) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1332) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1322) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at java.base/java.util.concurrent.FutureTask.run$$$capture(FutureTask.java:264) ~[na:na]
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java) ~[na:na]
	at org.apache.tomcat.util.threads.InlineExecutorService.execute(InlineExecutorService.java:75) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at java.base/java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:145) ~[na:na]
	at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:866) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.StandardEngine.startInternal(StandardEngine.java:240) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.StandardService.startInternal(StandardService.java:433) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.core.StandardServer.startInternal(StandardServer.java:921) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:171) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.apache.catalina.startup.Tomcat.start(Tomcat.java:437) ~[tomcat-embed-core-10.1.20.jar:10.1.20]
	at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.initialize(TomcatWebServer.java:126) ~[spring-boot-3.2.5.jar:3.2.5]
	... 13 common frames omitted
Caused by: java.lang.IllegalStateException: seckill.security.sign-secret 必须显式配置且长度不少于 32 字符，当前配置不合规
	at com.seckill.mall.security.ReplayProtectionFilter.validateSignSecret(ReplayProtectionFilter.java:61) ~[classes/:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[na:na]
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:568) ~[na:na]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMethod.invoke(InitDestroyAnnotationBeanPostProcessor.java:457) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMetadata.invokeInitMethods(InitDestroyAnnotationBeanPostProcessor.java:401) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:219) ~[spring-beans-6.1.6.jar:6.1.6]
	... 55 common frames omitted

已与地址为 ''127.0.0.1:58532'，传输: '套接字'' 的目标虚拟机断开连接

进程已结束，退出代码为 1
