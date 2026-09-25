#!/bin/bash
sed -i '' 's/public void recordClick(Link link/public void recordClick(java.util.UUID linkId/g' src/main/java/com/gupta/linkly/service/AnalyticsService.java
sed -i '' 's/linkRepository.incrementClickCount(link.getId());/linkRepository.incrementClickCount(linkId);/g' src/main/java/com/gupta/linkly/service/AnalyticsService.java
sed -i '' 's/\.link(link)/.link(linkRepository.getReferenceById(linkId))/g' src/main/java/com/gupta/linkly/service/AnalyticsService.java
sed -i '' 's/link.getId()/linkId/g' src/main/java/com/gupta/linkly/service/AnalyticsService.java
