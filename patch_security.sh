#!/bin/bash
sed -i '' 's/\.requestMatchers("\/api\/system\/\*\*")\.permitAll()/\.requestMatchers("\/api\/system\/\*\*")\.hasRole("ADMIN")/g' src/main/java/com/gupta/linkly/security/SecurityConfig.java
sed -i '' 's/\.requestMatchers("\/actuator\/\*\*")\.permitAll()/\.requestMatchers("\/actuator\/health")\.permitAll()\n                        \.requestMatchers("\/actuator\/\*\*")\.hasRole("ADMIN")/g' src/main/java/com/gupta/linkly/security/SecurityConfig.java

cat << 'INNER_EOF' > replacement_cors.txt
        String frontendUrl = System.getenv("FRONTEND_URL");
        if (frontendUrl != null && !frontendUrl.isEmpty()) {
            configuration.setAllowedOrigins(java.util.List.of(frontendUrl, "http://localhost:5173"));
            configuration.setAllowCredentials(true);
        } else {
            configuration.setAllowedOrigins(java.util.List.of("http://localhost:5173"));
            configuration.setAllowCredentials(true);
        }
INNER_EOF

perl -i -0pe 's/String frontendUrl = System\.getenv\("FRONTEND_URL"\);\n.*?configuration\.setAllowedOriginPatterns\(java\.util\.List\.of\("\*"\)\);\n        \}/`cat replacement_cors.txt`/es' src/main/java/com/gupta/linkly/security/SecurityConfig.java
