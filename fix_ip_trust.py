with open('src/main/java/com/gupta/linkly/security/RateLimitingFilter.java', 'r') as f:
    content = f.read()

content = content.replace(
    '    private String getClientIP(HttpServletRequest request) {\n        String xfHeader = request.getHeader("X-Forwarded-For");\n        if (xfHeader == null || xfHeader.isEmpty()) {\n            return request.getRemoteAddr();\n        }\n        return xfHeader.split(",")[0];\n    }',
    '    private String getClientIP(HttpServletRequest request) {\n        return request.getRemoteAddr();\n    }'
)
with open('src/main/java/com/gupta/linkly/security/RateLimitingFilter.java', 'w') as f:
    f.write(content)

with open('src/main/java/com/gupta/linkly/controller/RedirectController.java', 'r') as f:
    content = f.read()

content = content.replace(
    '        String ip = request.getHeader("X-Forwarded-For");\n        if (ip == null || ip.isEmpty()) {\n            ip = request.getRemoteAddr();\n        } else {\n            ip = ip.split(",")[0];\n        }',
    '        String ip = request.getRemoteAddr();'
)
with open('src/main/java/com/gupta/linkly/controller/RedirectController.java', 'w') as f:
    f.write(content)

with open('src/main/java/com/gupta/linkly/controller/AuthController.java', 'r') as f:
    content = f.read()

content = content.replace(
    '    private boolean isSecure(jakarta.servlet.http.HttpServletRequest request) {\n        String xForwardedProto = request.getHeader("X-Forwarded-Proto");\n        if (xForwardedProto != null) {\n            return xForwardedProto.contains("https");\n        }\n        return request.isSecure() || !request.getServerName().equals("localhost");\n    }',
    '    private boolean isSecure(jakarta.servlet.http.HttpServletRequest request) {\n        return request.isSecure() || !request.getServerName().equals("localhost");\n    }'
)
with open('src/main/java/com/gupta/linkly/controller/AuthController.java', 'w') as f:
    f.write(content)

