with open('src/main/java/com/gupta/linkly/service/AnalyticsService.java', 'r') as f:
    content = f.read()

# Move the increment down
content = content.replace(
    '            linkRepository.incrementClickCount(linkId);\n            \n\n            // Ignore localhost/internal IPs\n            if (ip == null || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {\n                return;\n            }',
    '            // Ignore localhost/internal IPs\n            if (ip == null || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {\n                return;\n            }\n\n            // Increment simple counter\n            linkRepository.incrementClickCount(linkId);'
)

with open('src/main/java/com/gupta/linkly/service/AnalyticsService.java', 'w') as f:
    f.write(content)
