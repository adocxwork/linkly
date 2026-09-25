#!/bin/bash
sed -i '' 's/passwordEncoder.encode("admin")/passwordEncoder.encode(System.getenv("ADMIN_PASSWORD") != null ? System.getenv("ADMIN_PASSWORD") : java.util.UUID.randomUUID().toString().substring(0, 8))/g' src/main/java/com/gupta/linkly/LinklyApplication.java
sed -i '' 's/System.out.println("Admin user seeded successfully.");/System.out.println("Admin user seeded. Use ADMIN_PASSWORD env var to login or check DB.");/g' src/main/java/com/gupta/linkly/LinklyApplication.java
