#!/bin/bash
sed -i '' '/private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;/a\
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;\
' src/main/java/com/gupta/linkly/service/UserService.java

cat << 'INNER_EOF' > replacement.txt
        if (user.getLinks() != null) {
            for (com.gupta.linkly.entity.Link link : user.getLinks()) {
                stringRedisTemplate.delete("redirect:" + link.getShortUrl());
            }
        }
        userRepository.delete(user);
INNER_EOF
perl -i -0pe 's/userRepository\.delete\(user\);/`cat replacement.txt`/es' src/main/java/com/gupta/linkly/service/UserService.java

cat << 'INNER_EOF' > replacement_suspend.txt
        user.setIsSuspended(!user.getIsSuspended());
        userRepository.save(user);
        if (user.getLinks() != null) {
            for (com.gupta.linkly.entity.Link link : user.getLinks()) {
                stringRedisTemplate.delete("redirect:" + link.getShortUrl());
            }
        }
INNER_EOF
perl -i -0pe 's/user\.setIsSuspended\(\!user\.getIsSuspended\(\)\);\n        userRepository\.save\(user\);/`cat replacement_suspend.txt`/es' src/main/java/com/gupta/linkly/service/UserService.java
