#!/bin/bash
# Replace link.setClickCount(...) with linkRepository.incrementClickCount(...)
# In AnalyticsService.java

sed -i '' 's/link.setClickCount(link.getClickCount() + 1);/linkRepository.incrementClickCount(link.getId());/g' src/main/java/com/gupta/linkly/service/AnalyticsService.java
sed -i '' 's/linkRepository.save(link);//g' src/main/java/com/gupta/linkly/service/AnalyticsService.java
