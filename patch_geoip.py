import re

with open('src/main/java/com/gupta/linkly/service/AnalyticsService.java', 'r') as f:
    content = f.read()

# Replace RestTemplate initialization
content = re.sub(
    r'private final RestTemplate restTemplate = new RestTemplate\(\);',
    '''private final RestTemplate restTemplate;
    private final java.util.Map<String, String[]> geoCache = new java.util.concurrent.ConcurrentHashMap<>();

    public AnalyticsService(LinkRepository linkRepository, ClickAnalyticsRepository analyticsRepository) {
        this.linkRepository = linkRepository;
        this.analyticsRepository = analyticsRepository;
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        this.restTemplate = new RestTemplate(factory);
    }''',
    content
)

# Replace the geo fetching block
geo_block = '''            // Fetch Geo Data
            String country = "Unknown";
            String city = "Unknown";
            try {
                if (geoCache.containsKey(ip)) {
                    String[] cached = geoCache.get(ip);
                    country = cached[0];
                    city = cached[1];
                } else {
                    String url = "https://get.geojs.io/v1/ip/geo/" + ip + ".json";
                    Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                    if (response != null && response.get("country") != null) {
                        country = (String) response.get("country");
                        city = (String) response.get("city");
                        if (geoCache.size() < 10000) geoCache.put(ip, new String[]{country, city});
                    }
                }
            } catch (Exception e) {
                // Log without exposing raw IP aggressively
                log.error("Failed to fetch geo-data for an IP");
            }'''

# Replace old geo block
content = re.sub(
    r'            // Fetch Geo Data.*?            } catch \(Exception e\) \{\n                log\.error\("Failed to fetch geo-data for IP \{\}: \{\}", ip, e\.getMessage\(\)\);\n            \}',
    geo_block,
    content,
    flags=re.DOTALL
)

with open('src/main/java/com/gupta/linkly/service/AnalyticsService.java', 'w') as f:
    f.write(content)
