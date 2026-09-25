import re

with open('src/main/java/com/gupta/linkly/service/AnalyticsStreamConsumer.java', 'r') as f:
    content = f.read()

# Add new dependencies
content = re.sub(
    r'private final LinkRepository linkRepository;',
    'private final LinkRepository linkRepository;\n    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;\n    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;',
    content
)

# Use objectMapper and remove log.info, add ack
content = re.sub(
    r'new com.fasterxml.jackson.databind.ObjectMapper\(\).readValue\(json, ClickEvent.class\);',
    'objectMapper.readValue(json, ClickEvent.class);',
    content
)

content = re.sub(
    r'log\.info\("Processed click event from stream for link: \{\}", event\.getLinkId\(\)\);',
    '// Processed successfully\n                    stringRedisTemplate.opsForStream().acknowledge("analytics-group", message);',
    content
)

with open('src/main/java/com/gupta/linkly/service/AnalyticsStreamConsumer.java', 'w') as f:
    f.write(content)
