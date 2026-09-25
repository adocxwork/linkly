#!/bin/bash
cat << 'INNER_EOF' > replacement.txt
    public String getOriginalUrlAndIncrementClick(String shortUrl, String ip, String userAgent) {
        String cacheKey = "redirect:" + shortUrl;
        String cachedData = stringRedisTemplate.opsForValue().get(cacheKey);
        
        java.util.UUID linkId;
        String originalUrl;

        if (cachedData != null) {
            String[] parts = cachedData.split("\\|");
            linkId = java.util.UUID.fromString(parts[0]);
            originalUrl = parts[1];
        } else {
            Link link = linkRepository.findByShortUrl(shortUrl)
                    .orElseThrow(() -> new ResourceNotFoundException("Link not found"));

            if (Boolean.FALSE.equals(link.getActive()) || Boolean.TRUE.equals(link.getUser().getIsSuspended())) {
                throw new ResourceNotFoundException("Link is inactive");
            }
            linkId = link.getId();
            originalUrl = link.getOriginalUrl();
            stringRedisTemplate.opsForValue().set(cacheKey, linkId + "|" + originalUrl, java.time.Duration.ofHours(24));
        }

        try {
            com.gupta.linkly.dto.ClickEvent event = new com.gupta.linkly.dto.ClickEvent(linkId, ip, userAgent);
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(event);
            stringRedisTemplate.opsForStream().add("link-clicks-stream", java.util.Collections.singletonMap("payload", json));
        } catch (Exception e) {
            analyticsService.recordClick(linkId, ip, userAgent);
        }

        return originalUrl;
    }
INNER_EOF

# We need to replace the exact method. We can use perl or python. Let's use perl.
perl -i -0pe 's/public String getOriginalUrlAndIncrementClick.*?return link.getOriginalUrl\(\);\n    \}/`cat replacement.txt`/es' src/main/java/com/gupta/linkly/service/LinkService.java
