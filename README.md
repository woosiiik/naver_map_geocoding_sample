# Naver Map Geocoding API Example

x : Longitude (경도)   
y : Latitude (위도)

## Geocoding
```java
// Address --> GPS coordinates.
String addr = "제주특별자치도 서귀포시 표선면 표선동서로 177-1 1층";
GeocodingResult result = geocoding("yourClientId", "yourClientSecret", addr, GEOCODING_URL);
System.out.println("주소 -> GPS 좌표");
System.out.println("입력 주소 : " + addr);
System.out.println("결과 : " + result);
```

## Reverse Geocoding
```java
// GPS coordinates --> Address
double y = 37.5485777;
double x = 126.8388089;
ReverseGeocodingResult result = reverseGeocoding("yourClientId", "yourClientSecret", x, y, REVERSE_GEOCODING_URL);
System.out.println("GPS 좌표 -> 주소");
System.out.println("GPS : " + x + ", " + y);
System.out.println("결과 : " + result);
```

## Calculate the distance between two GPS coordinates
```java
double x1 = 126.98827617645597;
double y1 = 37.56799459696302;
double x2 = 126.8292487;
double y2 = 33.3203441;
System.out.println("지점 A : " + x1 + ", " + y1 + " 주소:" + reverseGeocoding("yourClientId", "yourClientSecret", x1, y1, REVERSE_GEOCODING_URL).toString());
System.out.println("지점 B : " + x2 + ", " + y2 + " 주소:" + reverseGeocoding("yourClientId", "yourClientSecret", x2, y2, REVERSE_GEOCODING_URL).toString());
System.out.println("두 좌표값 사이의 직선거리 (meter): " + distance(x1, y1, x2, y2));
```