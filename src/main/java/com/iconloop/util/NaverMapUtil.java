package com.iconloop.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NaverMapUtil {

    // Naver geocoding URL
    private static final String GEOCODING_URL = "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=";
    // Naver reverse geocoding URL
    private static final String REVERSE_GEOCODING_URL = "https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?request=coordsToaddr&sourcecrs=epsg:4326&output=json&orders=addr,roadaddr&coords=";

    // Naver geocoding URL for government
    private static final String GEOCODING_URL_GOV = "https://naveropenapi.apigw.gov-ntruss.com/map-geocode/v2/geocode?query=";
    // Naver reverse geocoding URL for government
    private static final String REVERSE_GEOCODING_URL_GOV = "https://naveropenapi.apigw.gov-ntruss.com/map-reversegeocode/v2/gc?request=coordsToaddr&sourcecrs=epsg:4326&output=json&orders=addr,roadaddr&coords=";

    // Naver client id
    private static final String HEADER_NAME_CLIENT_ID = "X-NCP-APIGW-API-KEY-ID";
    // Naver client secret
    private static final String HEADER_NAME_CLIENT_SECRET = "X-NCP-APIGW-API-KEY";


    public static class GeocodingResult {
        /**
         * OK, INVALID_REQUEST, SYSTEM_ERROR, INVALID_ADDRESS
         */
        public String status;

        public String errorMessage;
        /**
         * 경도
         */
        public double x;
        /**
         * 위도
         */
        public double y;
        /**
         * 도로명 주소.
         */
        public String roadAddress;
        /**
         * 지번 주소.
         */
        public String jibunAddress;

        public String toString() {
            return "status:" + status + ", errorMessage=" + errorMessage + ", x=" + x + ", y=" + y + ", roadAddr=" + roadAddress + ", jibunAddr=" + jibunAddress;
        }
    }

    /**
     * 주소를 입력 받아서 GPS좌표값을 리턴한다.
     * https://apidocs.ncloud.com/ko/ai-naver/maps_geocoding/geocode/
     * *
     *
     * @param clientId     Naver Client ID
     * @param clientSecret Naver Client Secret
     * @param address      Address to convert to GPS.
     * @return GeocodingResult
     * @throws IOException
     */
    public static GeocodingResult geocoding(String clientId, String clientSecret, String address, String geocodingUrl) throws IOException {

        String reqUrl = geocodingUrl + address;

        HashMap<String, String> headers = new HashMap<String, String>() {
            {
                put(HEADER_NAME_CLIENT_ID, clientId);
                put(HEADER_NAME_CLIENT_SECRET, clientSecret);
            }
        };

        String result = httpGet(reqUrl, headers);
        System.out.println("result :" + result);

        GeocodingResult geocodingResult = new GeocodingResult();

        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(result);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        geocodingResult.status = jsonObject.get("status").getAsString();
        geocodingResult.errorMessage = jsonObject.get("errorMessage").getAsString();

        if (!geocodingResult.status.equalsIgnoreCase("OK")) {
            return geocodingResult;
        }

        JsonObject metaObject = jsonObject.get("meta").getAsJsonObject();
        if (metaObject != null) {
            int totalCount = metaObject.get("totalCount").getAsInt();
            if (totalCount < 1) {
                // OK 이더라도 totalcount 가 0 이면 못가져온 것이고, 이 경우 주소 검색이 안 된 경우이다.
                geocodingResult.status = "INVALID_ADDRESS";
                return geocodingResult;
            }
            int count = metaObject.get("count").getAsInt();
            int page = metaObject.get("page").getAsInt();
        }

        // 주소가 혹시 여러개가 오더라도 어떤 것을 선택하거나 하게 할 수 없다. 그냥 첫번째 것을 사용하겠다.
        JsonArray addressArray = jsonObject.get("addresses").getAsJsonArray();
        JsonElement addressEle = addressArray.get(0);
        JsonObject addressObject = addressEle.getAsJsonObject();
        geocodingResult.jibunAddress = addressObject.get("jibunAddress").getAsString();
        geocodingResult.roadAddress = addressObject.get("roadAddress").getAsString();
        String xString = addressObject.get("x").getAsString();
        String yString = addressObject.get("y").getAsString();
        geocodingResult.x = Double.parseDouble(xString);
        geocodingResult.y = Double.parseDouble(yString);

        return geocodingResult;
    }

    /**
     * Result for Reverse Geocoding.
     */
    public static class ReverseGeocodingResult {
        /**
         * 0 = 성공.
         */
        public int statusCode;
        public String statusName;
        public String statusMessage;

        /**
         * 도로명 주소. (없을 수도 있음. 없는 경우 아래 지번 주소를 사용.)
         */
        public String roadAddress;
        /**
         * 지번 주소.
         */
        public String jibunAddress;

        public String toString() {
            return "code:" + statusCode + ", name:" + statusName + ", message:" + statusMessage + ", roadAddress:" + roadAddress + ", jibunAddress:" + jibunAddress;
        }
    }

    /**
     * GPS 좌표값으로 주소 정보를 리턴.
     * https://apidocs.ncloud.com/ko/ai-naver/maps_reverse_geocoding/gc/
     *
     * @param clientId
     * @param clientSecret
     * @param x
     * @param y
     * @return
     * @throws IOException
     */
    public static ReverseGeocodingResult reverseGeocoding(String clientId, String clientSecret, double x, double y, String reverseGeocodingUrl) throws IOException {
        String reqUrl = reverseGeocodingUrl + String.valueOf(x) + "," + String.valueOf(y);
        HashMap<String, String> headers = new HashMap<String, String>() {
            {
                put(HEADER_NAME_CLIENT_ID, clientId);
                put(HEADER_NAME_CLIENT_SECRET, clientSecret);
            }
        };

        String result = httpGet(reqUrl, headers);
        ReverseGeocodingResult geocodingResult = new ReverseGeocodingResult();
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(result);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonObject statusObject = jsonObject.get("status").getAsJsonObject();
        geocodingResult.statusCode = statusObject.get("code").getAsInt();
        geocodingResult.statusMessage = statusObject.get("message").getAsString();
        geocodingResult.statusName = statusObject.get("name").getAsString();

        if (geocodingResult.statusCode != 0) {
            return geocodingResult;
        }

        JsonArray jsonArray = jsonObject.get("results").getAsJsonArray();
        int size = jsonArray.size();
        for (int i = 0; i < size; i++) {
            JsonObject jsonAddrObject = jsonArray.get(i).getAsJsonObject();
            String addName = jsonAddrObject.get("name").getAsString();

            StringBuffer fullAddress = new StringBuffer();

            JsonObject regionObject = jsonAddrObject.get("region").getAsJsonObject();

            String area1Name = regionObject.get("area1").getAsJsonObject().get("name").getAsString();
            String area2Name = regionObject.get("area2").getAsJsonObject().get("name").getAsString();
            String area3Name = regionObject.get("area3").getAsJsonObject().get("name").getAsString();
            String area4Name = regionObject.get("area4").getAsJsonObject().get("name").getAsString();

            fullAddress.append(area1Name).append(" ");
            fullAddress.append(area2Name).append(" ");
            fullAddress.append(area3Name).append(" ");
            if (area4Name != null && area4Name.length() > 0) {
                fullAddress.append(area4Name).append(" ");
            }

            JsonObject landObject = jsonAddrObject.get("land").getAsJsonObject();
            String number1 = landObject.get("number1").getAsString();
            String number2 = landObject.get("number2").getAsString();

            if (addName.equalsIgnoreCase("addr")) {
                // 지번주소
                fullAddress.append(number1);
                if (number2 != null && number2.length() > 0) {
                    fullAddress.append("-").append(number2);
                }
                geocodingResult.jibunAddress = fullAddress.toString();
            } else if (addName.equalsIgnoreCase("roadaddr")) {
                // 도로명주소
                String doroName = landObject.get("name").getAsString();
                fullAddress.append(doroName).append(" ").append(number1);
                if (number2 != null && number2.length() > 0) {
                    fullAddress.append("-").append(number2);
                }

                String addition0Value = landObject.get("addition0").getAsJsonObject().get("value").getAsString();
                if (addition0Value != null) {
                    fullAddress.append(" ").append(addition0Value);
                }

                geocodingResult.roadAddress = fullAddress.toString();
            }
        }

        return geocodingResult;
    }

    /**
     * 두 지점간의 거리(meter) 계산
     *
     * @param y1 지점 1 경도
     * @param x1 지점 1 위도
     * @param y2 지점 2 경도
     * @param x2 지점 2 위도
     * @return
     */
    public static double distance(double x1, double y1, double x2, double y2) {

        double theta = x1 - x2;
        double dist = Math.sin(deg2rad(y1)) * Math.sin(deg2rad(y2)) + Math.cos(deg2rad(y1)) * Math.cos(deg2rad(y2)) * Math.cos(deg2rad(theta));

        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        // Meter
        dist = dist * 1609.344;
        // Kilometer
        // dist = dist * 1.609344;
        return dist;
    }


    // This function converts decimal degrees to radians
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    // This function converts radians to decimal degrees
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    public static String httpGet(String requestURL, Map<String, String> headers) throws IOException {
        String message = null;
        OkHttpClient client = new OkHttpClient();

        Request.Builder builder = new Request.Builder();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                builder.addHeader(key, value);
            }
        }
        builder.url(requestURL);

        Request request = builder.build();

        Response response = client.newCall(request).execute();
        message = response.body().string();
        response.close();
        return message;
    }


    public static void main(String... args) {

        System.out.println("\n\n\n ################  공공기관용 ##################");

        // 주소 --> GPS 좌표.
        String addr = "제주특별자치도 서귀포시 표선면 표선동서로 177-1 1층";
        try {
            GeocodingResult result = geocoding("yourGovClientId", "yourGovClientSecret", addr, GEOCODING_URL_GOV);
            System.out.println("========================================================================================================================");
            System.out.println("주소 -> GPS 좌표");
            System.out.println("입력 주소 : " + addr);
            System.out.println("결과 : " + result);
            System.out.println("========================================================================================================================");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // GPS 좌표 --> 주소

        try {
            double y = 37.5485777;
            double x = 126.8388089;
            ReverseGeocodingResult result = reverseGeocoding("yourGovClientId", "yourGovClientSecret", x, y, REVERSE_GEOCODING_URL_GOV);
            System.out.println("========================================================================================================================");
            System.out.println("GPS 좌표 -> 주소");
            System.out.println("GPS : " + x + ", " + y);
            System.out.println("결과 : " + result);
            System.out.println("========================================================================================================================");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            // GPS 좌표간 거리 계산.

            double x1 = 126.98827617645597;
            double y1 = 37.56799459696302;
            double x2 = 126.8292487;
            double y2 = 33.3203441;
            System.out.println("========================================================================================================================");
            System.out.println("직선거리 계산 ");
            System.out.println("지점 A : " + x1 + ", " + y1 + " 주소:" + reverseGeocoding("yourGovClientId", "yourGovClientSecret", x1, y1, REVERSE_GEOCODING_URL_GOV).toString());
            System.out.println("지점 B : " + x2 + ", " + y2 + " 주소:" + reverseGeocoding("yourGovClientId", "yourGovClientSecret", x2, y2, REVERSE_GEOCODING_URL_GOV).toString());
            System.out.println("두 좌표값 사이의 직선거리 (meter): " + distance(x1, y1, x2, y2));
            System.out.println("========================================================================================================================");
        } catch (IOException e) {
            e.printStackTrace();
        }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        System.out.println("\n\n\n ################  일반 사업자 ##################");

        try {
            GeocodingResult result = geocoding("yourClientId", "yourClientSecret", addr, GEOCODING_URL);
            System.out.println("========================================================================================================================");
            System.out.println("주소 -> GPS 좌표");
            System.out.println("입력 주소 : " + addr);
            System.out.println("결과 : " + result);
            System.out.println("========================================================================================================================");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // GPS 좌표 --> 주소

        try {
            double y = 37.5485777;
            double x = 126.8388089;
            ReverseGeocodingResult result = reverseGeocoding("yourClientId", "yourClientSecret", x, y, REVERSE_GEOCODING_URL);
            System.out.println("========================================================================================================================");
            System.out.println("GPS 좌표 -> 주소");
            System.out.println("GPS : " + x + ", " + y);
            System.out.println("결과 : " + result);
            System.out.println("========================================================================================================================");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            // GPS 좌표간 거리 계산.

            double x1 = 126.98827617645597;
            double y1 = 37.56799459696302;
            double x2 = 126.8292487;
            double y2 = 33.3203441;
            System.out.println("========================================================================================================================");
            System.out.println("직선거리 계산 ");
            System.out.println("지점 A : " + x1 + ", " + y1 + " 주소:" + reverseGeocoding("yourClientId", "yourClientSecret", x1, y1, REVERSE_GEOCODING_URL).toString());
            System.out.println("지점 B : " + x2 + ", " + y2 + " 주소:" + reverseGeocoding("yourClientId", "yourClientSecret", x2, y2, REVERSE_GEOCODING_URL).toString());
            System.out.println("두 좌표값 사이의 직선거리 (meter): " + distance(x1, y1, x2, y2));
            System.out.println("========================================================================================================================");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
