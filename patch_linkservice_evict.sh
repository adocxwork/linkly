#!/bin/bash
sed -i '' '/link.setOriginalUrl(request.getOriginalUrl());/a\
        stringRedisTemplate.delete("redirect:" + link.getShortUrl());' src/main/java/com/gupta/linkly/service/LinkService.java

sed -i '' '/linkRepository.delete(link);/a\
        stringRedisTemplate.delete("redirect:" + link.getShortUrl());' src/main/java/com/gupta/linkly/service/LinkService.java
