package sample;

import com.google.gson.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.List;
import java.util.Map;


public class Main extends Application {
    FileChooser fileChooser = new FileChooser();
    TextField textField;
    Stage primaryStage;
    String fileName = "excel.json";
    File choseFile = new File(fileName);
    Gson gson = new Gson();

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Excel2Json");
        primaryStage.setScene(new Scene(root, 500, 275));
        primaryStage.show();

        this.primaryStage = primaryStage;
        textField = (TextField) root.lookup("#path");
        Button choseButton = (Button) root.lookup("#chose");
        Button btn = (Button) root.lookup("#btn");

        choseButton.setOnAction(event -> {
            choseFile = fileChooser.showOpenDialog(primaryStage);
            if (choseFile != null) {
                textField.setText(choseFile.getAbsolutePath());
            }
        });
        btn.setOnAction(event -> {
            try {
                String str = gehuaJsonIdcheck(choseFile);
                System.out.println("hefeiExcel2Json=" + str);
                makeFile(str);
            } catch (Exception e) {
                showTip(e.getMessage());
            }
        });
    }

    public String gehuaJsonIdcheck(File file) {
        String json = readtFile(file);
        System.out.println("json=" + json);
        ChannelList channelList = gson.fromJson(json, ChannelList.class);
        List<ChannelList.ChannelListBean.ChannelsBean> channelsBeans = channelList.getChannelList().getChannels();
        for (ChannelList.ChannelListBean.ChannelsBean channelsBean : channelsBeans) {
            if (!channelsBean.getId().equals(channelsBean.getParams().getEXT_PARAM().getServiceId())) {
                channelsBean.setId(channelsBean.getParams().getEXT_PARAM().getServiceId());
                System.out.println("channelName=" + channelsBean.getChannelName() + " setId=" + channelsBean.getParams().getEXT_PARAM().getServiceId());
            }
            if (channelsBean.getLid() == null || channelsBean.getLid().length() <= 0) {
                channelsBean.setLid(channelsBean.getParams().getEXT_PARAM().getTsID());
                System.out.println("channelName=" + channelsBean.getChannelName() + " setLid=" + channelsBean.getParams().getEXT_PARAM().getTsID());
            }
            for (ChannelList.ChannelListBean.ChannelsBean bean : channelsBeans) {
                if (channelsBean.getId().equals(bean.getId()) && !channelsBean.getChannelName().equals(bean.getChannelName())) {
                    System.out.println("id重复 " + channelsBean.getChannelName() + "," + bean.getChannelName());
                }
                if (channelsBean.getLid().equals(bean.getLid()) && !channelsBean.getChannelName().equals(bean.getChannelName())) {
                    System.out.println("lid重复 " + channelsBean.getChannelName() + "," + bean.getChannelName());
                }
            }
        }
        return gson.toJson(channelList);
    }

    /**
     * 读取数据，存入集合中
     */
    public static String readtFile(File file) {
        String ret = "";
        InputStreamReader read = null;// 考虑到编码格式
        try {
            read = new InputStreamReader(new FileInputStream(file), "utf-8");

            BufferedReader bufferedReader = new BufferedReader(read);
            String lineTxt = null;
            while ((lineTxt = bufferedReader.readLine()) != null) {
                ret += lineTxt + "\n";
            }
            read.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public String hefeiExcel2Json(Map<Integer, Map<Integer, Object>> excelMap) {

        JsonArray channelsJson = new JsonArray();
        for (int i = 0; i < excelMap.size(); i++) {
            Map<Integer, Object> line = excelMap.get(i);
            JsonObject channelJson = new JsonObject();
            channelJson.addProperty("channelName", obj2String(line.get(0)));
            channelJson.addProperty("id", obj2String(line.get(1)));
            channelJson.addProperty("lid", obj2String(line.get(2)));
            JsonObject channelParam = new JsonObject();
            channelJson.add("extParams", channelParam);

            channelParam.addProperty("frequency", obj2String(line.get(3)));
            channelParam.addProperty("sid", obj2String(line.get(4)));
            channelsJson.add(channelJson);
        }

        return gson.toJson(channelsJson);
    }


    public String gehuaExcel2Json(Map<Integer, Map<Integer, Object>> excelMap) {
        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        JsonArray channels = jsonObject.getAsJsonObject("channelList").getAsJsonArray("channels");
        for (int i = 0; i < channels.size(); i++) {
            JsonObject channelObj = channels.get(i).getAsJsonObject();
            String serviceId = channelObj.getAsJsonObject("params").getAsJsonObject("EXT_PARAM").get("serviceId").getAsString();
            channelObj.addProperty("id", serviceId);
            for (int j = 0; j < excelMap.size(); j++) {
                Map<Integer, Object> row = excelMap.get(j);
                String map_sid = obj2String(row.get(2));
                if (serviceId.equals(map_sid)) {
                    channelObj.addProperty("lid", obj2String(row.get(0)));
                }
            }
            JsonElement par = channelObj.get("params");
            channelObj.remove("params");
            channelObj.add("params", par);
            if (channelObj.get("lid") == null) {
                System.out.println("serviceId=" + serviceId + " lid为空");
                channels.remove(i);
            }
        }
        return gson.toJson(jsonObject);
    }


    public void showTip(String msg) {
        Alert alert = new Alert(Alert.AlertType.NONE, msg, ButtonType.CLOSE);
        alert.show();
    }

    public Map<Integer, Map<Integer, Object>> getMaps(int sheetIndex) {
        ReadExcelUtils readExcelUtils = new ReadExcelUtils(textField.getText());
        Map<Integer, Map<Integer, Object>> map = null;
        try {
            map = readExcelUtils.readExcelContent(sheetIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * 按呼词规则创建jSOn
     *
     * @param map
     * @return
     */
    public String createHuCiJson(Map<Integer, Map<Integer, Object>> map) {
        fileName = choseFile.getName() + "呼词.json";
        JsonArray jsonArray = new JsonArray();
        for (int i = 1; i < map.size(); i++) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", obj2String(map.get(i).get(0)));
            jsonObject.addProperty("module", "Channel");
            if (map.get(i).size() >= 3) {
                String keys = "";
                for (int j = 2; j < map.get(i).size(); j++) {
                    String key = obj2String(map.get(i).get(j));
                    if (!key.isEmpty()) {
                        keys += key + ",";
                    }
                }
                if (keys.endsWith(",")) {
                    keys = keys.substring(0, keys.length() - 1);
                }
                jsonObject.addProperty("keys", keys);
            }
            jsonArray.add(jsonObject);
        }
        String json = jsonArray.toString();
        return json;
    }

    /**
     * 对读取的数据做一些处理
     *
     * @param obj
     * @return
     */
    public String obj2String(Object obj) {
        String value;
        if (obj instanceof Double) {
            double doubleValue = (double) obj;
            if (doubleValue * 10 % 10 == 0) {
                int intVaule = (int) doubleValue;
                value = String.valueOf(intVaule);
            } else {
                value = String.valueOf(doubleValue);
            }
        } else {
            value = (String) obj;
        }
        return value.trim();
    }

    /**
     * 按照频道列表规则Json（就是第一行是 key）
     *
     * @param map
     * @return
     */
    public String createString(Map<Integer, Map<Integer, Object>> map) {
        fileName = choseFile.getName() + "频道列表.json";
        if (map.size() >= 2) {
            Map<Integer, Object> firstRow = map.get(0);
            JsonArray jsonArray = new JsonArray();
            for (int i = 1; i < map.size(); i++) {
                JsonObject jsonObject = new JsonObject();
                for (int j = 0; j < firstRow.size(); j++) {
                    String key = obj2String(firstRow.get(j));
                    Object obj = map.get(i).get(j);
                    String value = obj2String(obj);
                    if (key.equals("url")) {
                        jsonObject.addProperty("url", "cbn://gotoIpLive?channelId=" + value.trim());
                    } else if (key.equals("id") && value.isEmpty()) {
                        jsonObject.addProperty(key, "1000" + i);
                    } else if (!key.isEmpty()) {
                        jsonObject.addProperty(key, value);
                    }
                }
                jsonArray.add(jsonObject);
            }
            return jsonArray.toString();
        } else {
            return null;
        }

    }

    public void makeFile(String json) {
        try {
            System.out.println("json:" + json);
            fileChooser.setInitialFileName(fileName);
            File mfile = fileChooser.showSaveDialog(primaryStage);
            if (mfile != null) {
                // 将格式化后的字符串写入文件
                Writer write = new OutputStreamWriter(new FileOutputStream(mfile), "UTF-8");
                write.write(json);
                write.flush();
                write.close();
                showTip("转出成功！");
            } else {
                showTip("路径选择出错！");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showTip("出错了!错误信息:" + e.getMessage());
            System.out.println("出错了" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    String json = "{\n" +
            "  \"channelList\": {\n" +
            "    \"channels\": [\n" +
            "      {\n" +
            "        \"channelName\": \"BTV北京\",\n" +
            "        \"id\": \"101\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"2\",\n" +
            "            \"tsID\": \"21\",\n" +
            "            \"serviceId\": \"101\",\n" +
            "            \"frequency\": \"323\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"BTV文艺\",\n" +
            "        \"id\": \"102\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"2\",\n" +
            "            \"tsID\": \"22\",\n" +
            "            \"serviceId\": \"102\",\n" +
            "            \"frequency\": \"323\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"BTV科教\",\n" +
            "        \"id\": \"103\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"2\",\n" +
            "            \"tsID\": \"23\",\n" +
            "            \"serviceId\": \"103\",\n" +
            "            \"frequency\": \"323\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"BTV影视\",\n" +
            "        \"id\": \"104\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"2\",\n" +
            "            \"tsID\": \"24\",\n" +
            "            \"serviceId\": \"104\",\n" +
            "            \"frequency\": \"323\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"BTV财经\",\n" +
            "        \"id\": \"105\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"2\",\n" +
            "            \"tsID\": \"25\",\n" +
            "            \"serviceId\": \"105\",\n" +
            "            \"frequency\": \"323\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"BTV体育\",\n" +
            "        \"id\": \"106\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"2\",\n" +
            "            \"tsID\": \"26\",\n" +
            "            \"serviceId\": \"106\",\n" +
            "            \"frequency\": \"323\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"BTV生活\",\n" +
            "        \"id\": \"107\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"2\",\n" +
            "            \"tsID\": \"27\",\n" +
            "            \"serviceId\": \"107\",\n" +
            "            \"frequency\": \"323\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"BTV青年\",\n" +
            "        \"id\": \"108\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"2\",\n" +
            "            \"tsID\": \"28\",\n" +
            "            \"serviceId\": \"108\",\n" +
            "            \"frequency\": \"323\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"BTV新闻\",\n" +
            "        \"id\": \"109\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"2\",\n" +
            "            \"tsID\": \"29\",\n" +
            "            \"serviceId\": \"109\",\n" +
            "            \"frequency\": \"323\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"BTV KAKU\",\n" +
            "        \"id\": \"110\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"2\",\n" +
            "            \"tsID\": \"30\",\n" +
            "            \"serviceId\": \"110\",\n" +
            "            \"frequency\": \"331\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-1综合\",\n" +
            "        \"id\": \"121\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"1\",\n" +
            "            \"serviceId\": \"121\",\n" +
            "            \"frequency\": \"339\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-2财经\",\n" +
            "        \"id\": \"122\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"2\",\n" +
            "            \"serviceId\": \"122\",\n" +
            "            \"frequency\": \"339\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-3综艺\",\n" +
            "        \"id\": \"123\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"3\",\n" +
            "            \"serviceId\": \"123\",\n" +
            "            \"frequency\": \"331\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-4中文国际\",\n" +
            "        \"id\": \"124\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"4\",\n" +
            "            \"serviceId\": \"124\",\n" +
            "            \"frequency\": \"650\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-5体育\",\n" +
            "        \"id\": \"125\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"5\",\n" +
            "            \"serviceId\": \"125\",\n" +
            "            \"frequency\": \"331\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-6电影\",\n" +
            "        \"id\": \"126\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"6\",\n" +
            "            \"serviceId\": \"126\",\n" +
            "            \"frequency\": \"331\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-7军事农业\",\n" +
            "        \"id\": \"127\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"7\",\n" +
            "            \"serviceId\": \"127\",\n" +
            "            \"frequency\": \"339\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-8电视剧\",\n" +
            "        \"id\": \"128\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"8\",\n" +
            "            \"serviceId\": \"128\",\n" +
            "            \"frequency\": \"331\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-9纪录\",\n" +
            "        \"id\": \"237\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"9\",\n" +
            "            \"serviceId\": \"237\",\n" +
            "            \"frequency\": \"331\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-10科教\",\n" +
            "        \"id\": \"130\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"10\",\n" +
            "            \"serviceId\": \"130\",\n" +
            "            \"frequency\": \"339\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-11戏曲\",\n" +
            "        \"id\": \"825\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"11\",\n" +
            "            \"serviceId\": \"131\",\n" +
            "            \"frequency\": \"339\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-12社会与法\",\n" +
            "        \"id\": \"826\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"12\",\n" +
            "            \"serviceId\": \"132\",\n" +
            "            \"frequency\": \"339\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-13新闻\",\n" +
            "        \"id\": \"828\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"13\",\n" +
            "            \"serviceId\": \"133\",\n" +
            "            \"frequency\": \"331\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-14少儿\",\n" +
            "        \"id\": \"15\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"14\",\n" +
            "            \"serviceId\": \"134\",\n" +
            "            \"frequency\": \"331\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-15音乐\",\n" +
            "        \"id\": \"135\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"15\",\n" +
            "            \"serviceId\": \"135\",\n" +
            "            \"frequency\": \"339\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CGTN\",\n" +
            "        \"id\": \"26\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"16\",\n" +
            "            \"serviceId\": \"129\",\n" +
            "            \"frequency\": \"650\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CETV-3\",\n" +
            "        \"id\": \"829\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"40\",\n" +
            "            \"serviceId\": \"153\",\n" +
            "            \"frequency\": \"642\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CETV-1\",\n" +
            "        \"id\": \"151\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"1\",\n" +
            "            \"tsID\": \"41\",\n" +
            "            \"serviceId\": \"151\",\n" +
            "            \"frequency\": \"642\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"湖南卫视\",\n" +
            "        \"id\": \"180\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"31\",\n" +
            "            \"serviceId\": \"180\",\n" +
            "            \"frequency\": \"618\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"江苏卫视\",\n" +
            "        \"id\": \"183\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"32\",\n" +
            "            \"serviceId\": \"183\",\n" +
            "            \"frequency\": \"586\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"浙江卫视\",\n" +
            "        \"id\": \"182\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"33\",\n" +
            "            \"serviceId\": \"182\",\n" +
            "            \"frequency\": \"642\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"东方卫视\",\n" +
            "        \"id\": \"181\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"34\",\n" +
            "            \"serviceId\": \"181\",\n" +
            "            \"frequency\": \"618\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"深圳卫视\",\n" +
            "        \"id\": \"164\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"35\",\n" +
            "            \"serviceId\": \"210\",\n" +
            "            \"frequency\": \"562\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"云南卫视\",\n" +
            "        \"id\": \"34\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"36\",\n" +
            "            \"serviceId\": \"172\",\n" +
            "            \"frequency\": \"618\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"安徽卫视\",\n" +
            "        \"id\": \"189\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"37\",\n" +
            "            \"serviceId\": \"189\",\n" +
            "            \"frequency\": \"562\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"辽宁卫视\",\n" +
            "        \"id\": \"145\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"38\",\n" +
            "            \"serviceId\": \"163\",\n" +
            "            \"frequency\": \"618\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"山东卫视\",\n" +
            "        \"id\": \"153\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"39\",\n" +
            "            \"serviceId\": \"169\",\n" +
            "            \"frequency\": \"562\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"重庆卫视\",\n" +
            "        \"id\": \"171\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"42\",\n" +
            "            \"serviceId\": \"171\",\n" +
            "            \"frequency\": \"642\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"黑龙江卫视\",\n" +
            "        \"id\": \"143\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"43\",\n" +
            "            \"serviceId\": \"165\",\n" +
            "            \"frequency\": \"618\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"旅游卫视\",\n" +
            "        \"id\": \"188\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"44\",\n" +
            "            \"serviceId\": \"188\",\n" +
            "            \"frequency\": \"642\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"贵州卫视\",\n" +
            "        \"id\": \"173\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"45\",\n" +
            "            \"serviceId\": \"173\",\n" +
            "            \"frequency\": \"642\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"四川卫视\",\n" +
            "        \"id\": \"174\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"46\",\n" +
            "            \"serviceId\": \"174\",\n" +
            "            \"frequency\": \"586\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"江西卫视\",\n" +
            "        \"id\": \"185\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"47\",\n" +
            "            \"serviceId\": \"185\",\n" +
            "            \"frequency\": \"618\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"天津卫视\",\n" +
            "        \"id\": \"161\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"48\",\n" +
            "            \"serviceId\": \"161\",\n" +
            "            \"frequency\": \"586\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"广西卫视\",\n" +
            "        \"id\": \"187\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"49\",\n" +
            "            \"serviceId\": \"187\",\n" +
            "            \"frequency\": \"562\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"河南卫视\",\n" +
            "        \"id\": \"166\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"50\",\n" +
            "            \"serviceId\": \"166\",\n" +
            "            \"frequency\": \"562\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"广东卫视\",\n" +
            "        \"id\": \"186\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"51\",\n" +
            "            \"serviceId\": \"186\",\n" +
            "            \"frequency\": \"562\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"吉林卫视\",\n" +
            "        \"id\": \"830\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"52\",\n" +
            "            \"serviceId\": \"164\",\n" +
            "            \"frequency\": \"562\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"东南卫视\",\n" +
            "        \"id\": \"49\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"53\",\n" +
            "            \"serviceId\": \"184\",\n" +
            "            \"frequency\": \"618\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"湖北卫视\",\n" +
            "        \"id\": \"179\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"54\",\n" +
            "            \"serviceId\": \"179\",\n" +
            "            \"frequency\": \"562\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"陕西卫视\",\n" +
            "        \"id\": \"168\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"55\",\n" +
            "            \"serviceId\": \"168\",\n" +
            "            \"frequency\": \"562\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"宁夏卫视\",\n" +
            "        \"id\": \"177\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"56\",\n" +
            "            \"serviceId\": \"177\",\n" +
            "            \"frequency\": \"642\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"青海卫视\",\n" +
            "        \"id\": \"176\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"57\",\n" +
            "            \"serviceId\": \"176\",\n" +
            "            \"frequency\": \"586\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"甘肃卫视\",\n" +
            "        \"id\": \"178\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"58\",\n" +
            "            \"serviceId\": \"178\",\n" +
            "            \"frequency\": \"642\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"山东教育卫视\",\n" +
            "        \"id\": \"154\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"59\",\n" +
            "            \"serviceId\": \"154\",\n" +
            "            \"frequency\": \"642\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"河北卫视\",\n" +
            "        \"id\": \"162\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"60\",\n" +
            "            \"serviceId\": \"162\",\n" +
            "            \"frequency\": \"586\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"山西卫视\",\n" +
            "        \"id\": \"167\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"61\",\n" +
            "            \"serviceId\": \"167\",\n" +
            "            \"frequency\": \"586\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"内蒙古卫视\",\n" +
            "        \"id\": \"58\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"62\",\n" +
            "            \"serviceId\": \"175\",\n" +
            "            \"frequency\": \"586\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"新疆卫视\",\n" +
            "        \"id\": \"170\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"63\",\n" +
            "            \"serviceId\": \"170\",\n" +
            "            \"frequency\": \"618\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"西藏卫视\",\n" +
            "        \"id\": \"190\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"64\",\n" +
            "            \"serviceId\": \"190\",\n" +
            "            \"frequency\": \"586\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"厦门卫视\",\n" +
            "        \"id\": \"193\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"76\",\n" +
            "            \"serviceId\": \"193\",\n" +
            "            \"frequency\": \"618\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"兵团卫视\",\n" +
            "        \"id\": \"62\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"3\",\n" +
            "            \"tsID\": \"82\",\n" +
            "            \"serviceId\": \"194\",\n" +
            "            \"frequency\": \"586\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-1综合高清\",\n" +
            "        \"id\": \"601\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"601\",\n" +
            "            \"serviceId\": \"601\",\n" +
            "            \"frequency\": \"578\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-2财经高清\",\n" +
            "        \"id\": \"602\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"602\",\n" +
            "            \"serviceId\": \"602\",\n" +
            "            \"frequency\": \"355\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-3综艺高清\",\n" +
            "        \"id\": \"603\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"603\",\n" +
            "            \"serviceId\": \"603\",\n" +
            "            \"frequency\": \"578\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-4中文国际高清\",\n" +
            "        \"id\": \"604\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"604\",\n" +
            "            \"serviceId\": \"604\",\n" +
            "            \"frequency\": \"363\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-5体育高清\",\n" +
            "        \"id\": \"605\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"605\",\n" +
            "            \"serviceId\": \"605\",\n" +
            "            \"frequency\": \"578\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-6电影高清\",\n" +
            "        \"id\": \"606\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"606\",\n" +
            "            \"serviceId\": \"606\",\n" +
            "            \"frequency\": \"363\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-7军事农业高清\",\n" +
            "        \"id\": \"607\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"607\",\n" +
            "            \"serviceId\": \"607\",\n" +
            "            \"frequency\": \"355\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-8电视剧高清\",\n" +
            "        \"id\": \"608\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"608\",\n" +
            "            \"serviceId\": \"608\",\n" +
            "            \"frequency\": \"411\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-9纪录高清\",\n" +
            "        \"id\": \"609\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"609\",\n" +
            "            \"serviceId\": \"609\",\n" +
            "            \"frequency\": \"363\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-10科教高清\",\n" +
            "        \"id\": \"610\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"610\",\n" +
            "            \"serviceId\": \"610\",\n" +
            "            \"frequency\": \"355\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-12社会与法高清\",\n" +
            "        \"id\": \"612\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"612\",\n" +
            "            \"serviceId\": \"612\",\n" +
            "            \"frequency\": \"355\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-14少儿高清\",\n" +
            "        \"id\": \"614\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"614\",\n" +
            "            \"serviceId\": \"614\",\n" +
            "            \"frequency\": \"355\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CCTV-5+体育赛事\",\n" +
            "        \"id\": \"619\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"617\",\n" +
            "            \"serviceId\": \"617\",\n" +
            "            \"frequency\": \"578\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"歌华导视高清\",\n" +
            "        \"id\": \"76\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"618\",\n" +
            "            \"serviceId\": \"521\",\n" +
            "            \"frequency\": \"403\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"BTV纪实高清\",\n" +
            "        \"id\": \"620\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"620\",\n" +
            "            \"serviceId\": \"620\",\n" +
            "            \"frequency\": \"602\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"BTV北京卫视高清\",\n" +
            "        \"id\": \"621\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"621\",\n" +
            "            \"serviceId\": \"621\",\n" +
            "            \"frequency\": \"602\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"BTV文艺高清\",\n" +
            "        \"id\": \"599\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"622\",\n" +
            "            \"serviceId\": \"622\",\n" +
            "            \"frequency\": \"602\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"BTV影视高清\",\n" +
            "        \"id\": \"624\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"624\",\n" +
            "            \"serviceId\": \"624\",\n" +
            "            \"frequency\": \"419\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"BTV体育高清\",\n" +
            "        \"id\": \"626\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"626\",\n" +
            "            \"serviceId\": \"626\",\n" +
            "            \"frequency\": \"602\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"BTV新闻高清\",\n" +
            "        \"id\": \"629\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"629\",\n" +
            "            \"serviceId\": \"629\",\n" +
            "            \"frequency\": \"419\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"湖南卫视高清\",\n" +
            "        \"id\": \"631\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"631\",\n" +
            "            \"serviceId\": \"631\",\n" +
            "            \"frequency\": \"427\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"江苏卫视高清\",\n" +
            "        \"id\": \"632\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"632\",\n" +
            "            \"serviceId\": \"632\",\n" +
            "            \"frequency\": \"435\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"浙江卫视高清\",\n" +
            "        \"id\": \"633\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"633\",\n" +
            "            \"serviceId\": \"633\",\n" +
            "            \"frequency\": \"435\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"东方卫视高清\",\n" +
            "        \"id\": \"634\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"634\",\n" +
            "            \"serviceId\": \"634\",\n" +
            "            \"frequency\": \"435\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"深圳卫视高清\",\n" +
            "        \"id\": \"665\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"635\",\n" +
            "            \"serviceId\": \"665\",\n" +
            "            \"frequency\": \"379\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"安徽卫视高清\",\n" +
            "        \"id\": \"637\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"637\",\n" +
            "            \"serviceId\": \"637\",\n" +
            "            \"frequency\": \"427\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"辽宁卫视高清\",\n" +
            "        \"id\": \"638\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"638\",\n" +
            "            \"serviceId\": \"638\",\n" +
            "            \"frequency\": \"411\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"山东卫视高清\",\n" +
            "        \"id\": \"653\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"639\",\n" +
            "            \"serviceId\": \"653\",\n" +
            "            \"frequency\": \"594\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CETV-1高清\",\n" +
            "        \"id\": \"641\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"641\",\n" +
            "            \"serviceId\": \"640\",\n" +
            "            \"frequency\": \"435\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"重庆卫视高清\",\n" +
            "        \"id\": \"642\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"642\",\n" +
            "            \"serviceId\": \"642\",\n" +
            "            \"frequency\": \"411\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"黑龙江卫视高清\",\n" +
            "        \"id\": \"643\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"643\",\n" +
            "            \"serviceId\": \"643\",\n" +
            "            \"frequency\": \"594\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"四川卫视高清\",\n" +
            "        \"id\": \"646\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"646\",\n" +
            "            \"serviceId\": \"646\",\n" +
            "            \"frequency\": \"427\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"江西卫视高清\",\n" +
            "        \"id\": \"95\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"647\",\n" +
            "            \"serviceId\": \"647\",\n" +
            "            \"frequency\": \"411\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"天津卫视高清\",\n" +
            "        \"id\": \"657\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"648\",\n" +
            "            \"serviceId\": \"657\",\n" +
            "            \"frequency\": \"594\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"河南卫视高清\",\n" +
            "        \"id\": \"97\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"650\",\n" +
            "            \"serviceId\": \"650\",\n" +
            "            \"frequency\": \"594\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"广东卫视高清\",\n" +
            "        \"id\": \"651\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"651\",\n" +
            "            \"serviceId\": \"651\",\n" +
            "            \"frequency\": \"379\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"东南卫视高清\",\n" +
            "        \"id\": \"99\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"653\",\n" +
            "            \"serviceId\": \"639\",\n" +
            "            \"frequency\": \"427\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"湖北卫视高清\",\n" +
            "        \"id\": \"654\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"654\",\n" +
            "            \"serviceId\": \"654\",\n" +
            "            \"frequency\": \"594\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"河北卫视高清\",\n" +
            "        \"id\": \"660\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"660\",\n" +
            "            \"serviceId\": \"660\",\n" +
            "            \"frequency\": \"435\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"环球购物高清\",\n" +
            "        \"id\": \"1021\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"677\",\n" +
            "            \"serviceId\": \"619\",\n" +
            "            \"frequency\": \"570\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"金鹰纪实高清\",\n" +
            "        \"id\": \"1031\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"690\",\n" +
            "            \"serviceId\": \"690\",\n" +
            "            \"frequency\": \"411\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"上海纪实高清\",\n" +
            "        \"id\": \"1041\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"691\",\n" +
            "            \"serviceId\": \"691\",\n" +
            "            \"frequency\": \"427\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CHC-电影高清\",\n" +
            "        \"id\": \"618\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"4\",\n" +
            "            \"tsID\": \"692\",\n" +
            "            \"serviceId\": \"618\",\n" +
            "            \"frequency\": \"379\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"朝阳卫视高清\",\n" +
            "        \"id\": \"1061\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"5\",\n" +
            "            \"tsID\": \"801\",\n" +
            "            \"serviceId\": \"801\",\n" +
            "            \"frequency\": \"395\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"海淀卫视高清\",\n" +
            "        \"id\": \"1071\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"5\",\n" +
            "            \"tsID\": \"802\",\n" +
            "            \"serviceId\": \"802\",\n" +
            "            \"frequency\": \"395\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"丰台卫视\",\n" +
            "        \"id\": \"1081\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"5\",\n" +
            "            \"tsID\": \"803\",\n" +
            "            \"serviceId\": \"803\",\n" +
            "            \"frequency\": \"395\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"石景山卫视高清\",\n" +
            "        \"id\": \"1091\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"5\",\n" +
            "            \"tsID\": \"804\",\n" +
            "            \"serviceId\": \"804\",\n" +
            "            \"frequency\": \"395\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"门头沟卫视\",\n" +
            "        \"id\": \"1101\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"5\",\n" +
            "            \"tsID\": \"805\",\n" +
            "            \"serviceId\": \"805\",\n" +
            "            \"frequency\": \"347\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"房山卫视\",\n" +
            "        \"id\": \"111\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"5\",\n" +
            "            \"tsID\": \"806\",\n" +
            "            \"serviceId\": \"806\",\n" +
            "            \"frequency\": \"347\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"通州卫视\",\n" +
            "        \"id\": \"112\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"5\",\n" +
            "            \"tsID\": \"807\",\n" +
            "            \"serviceId\": \"807\",\n" +
            "            \"frequency\": \"347\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"顺义卫视\",\n" +
            "        \"id\": \"113\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"5\",\n" +
            "            \"tsID\": \"808\",\n" +
            "            \"serviceId\": \"808\",\n" +
            "            \"frequency\": \"347\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"大兴卫视\",\n" +
            "        \"id\": \"114\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"5\",\n" +
            "            \"tsID\": \"809\",\n" +
            "            \"serviceId\": \"809\",\n" +
            "            \"frequency\": \"347\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"昌平卫视\",\n" +
            "        \"id\": \"115\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"5\",\n" +
            "            \"tsID\": \"810\",\n" +
            "            \"serviceId\": \"810\",\n" +
            "            \"frequency\": \"347\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"平谷卫视\",\n" +
            "        \"id\": \"116\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"5\",\n" +
            "            \"tsID\": \"811\",\n" +
            "            \"serviceId\": \"811\",\n" +
            "            \"frequency\": \"347\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"怀柔卫视\",\n" +
            "        \"id\": \"117\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"5\",\n" +
            "            \"tsID\": \"812\",\n" +
            "            \"serviceId\": \"812\",\n" +
            "            \"frequency\": \"347\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"密云卫视\",\n" +
            "        \"id\": \"1181\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"5\",\n" +
            "            \"tsID\": \"813\",\n" +
            "            \"serviceId\": \"813\",\n" +
            "            \"frequency\": \"347\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"延庆卫视\",\n" +
            "        \"id\": \"119\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"5\",\n" +
            "            \"tsID\": \"814\",\n" +
            "            \"serviceId\": \"814\",\n" +
            "            \"frequency\": \"347\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"环球购物卫视\",\n" +
            "        \"id\": \"120\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"6\",\n" +
            "            \"tsID\": \"19\",\n" +
            "            \"serviceId\": \"244\",\n" +
            "            \"frequency\": \"570\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"家有购物卫视\",\n" +
            "        \"id\": \"1211\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"6\",\n" +
            "            \"tsID\": \"66\",\n" +
            "            \"serviceId\": \"247\",\n" +
            "            \"frequency\": \"443\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"时尚购物卫视\",\n" +
            "        \"id\": \"1221\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"6\",\n" +
            "            \"tsID\": \"67\",\n" +
            "            \"serviceId\": \"248\",\n" +
            "            \"frequency\": \"443\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"快乐购物卫视\",\n" +
            "        \"id\": \"1231\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"6\",\n" +
            "            \"tsID\": \"68\",\n" +
            "            \"serviceId\": \"268\",\n" +
            "            \"frequency\": \"371\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"央广购物卫视\",\n" +
            "        \"id\": \"1241\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"6\",\n" +
            "            \"tsID\": \"69\",\n" +
            "            \"serviceId\": \"245\",\n" +
            "            \"frequency\": \"650\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"优购物卫视\",\n" +
            "        \"id\": \"1251\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"6\",\n" +
            "            \"tsID\": \"70\",\n" +
            "            \"serviceId\": \"246\",\n" +
            "            \"frequency\": \"443\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"家家购物卫视\",\n" +
            "        \"id\": \"1261\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"6\",\n" +
            "            \"tsID\": \"71\",\n" +
            "            \"serviceId\": \"209\",\n" +
            "            \"frequency\": \"371\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"东方购物卫视\",\n" +
            "        \"id\": \"1271\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"6\",\n" +
            "            \"tsID\": \"72\",\n" +
            "            \"serviceId\": \"251\",\n" +
            "            \"frequency\": \"570\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"中视购物卫视\",\n" +
            "        \"id\": \"1281\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"6\",\n" +
            "            \"tsID\": \"73\",\n" +
            "            \"serviceId\": \"206\",\n" +
            "            \"frequency\": \"690\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"好享购物卫视\",\n" +
            "        \"id\": \"1291\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"6\",\n" +
            "            \"tsID\": \"74\",\n" +
            "            \"serviceId\": \"155\",\n" +
            "            \"frequency\": \"570\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"风尚购物卫视\",\n" +
            "        \"id\": \"1301\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"6\",\n" +
            "            \"tsID\": \"75\",\n" +
            "            \"serviceId\": \"156\",\n" +
            "            \"frequency\": \"443\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"爱家购物卫视\",\n" +
            "        \"id\": \"131\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"6\",\n" +
            "            \"tsID\": \"81\",\n" +
            "            \"serviceId\": \"113\",\n" +
            "            \"frequency\": \"554\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"中国教育四套高清\",\n" +
            "        \"id\": \"132\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"65\",\n" +
            "            \"serviceId\": \"198\",\n" +
            "            \"frequency\": \"642\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"金鹰卡通卫视\",\n" +
            "        \"id\": \"133\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"77\",\n" +
            "            \"serviceId\": \"211\",\n" +
            "            \"frequency\": \"618\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"炫动卡通卫视\",\n" +
            "        \"id\": \"134\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"78\",\n" +
            "            \"serviceId\": \"195\",\n" +
            "            \"frequency\": \"586\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"央广健康卫视\",\n" +
            "        \"id\": \"1351\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"88\",\n" +
            "            \"serviceId\": \"208\",\n" +
            "            \"frequency\": \"650\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"百姓健康卫视\",\n" +
            "        \"id\": \"136\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"89\",\n" +
            "            \"serviceId\": \"311\",\n" +
            "            \"frequency\": \"650\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"国学卫视\",\n" +
            "        \"id\": \"137\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"91\",\n" +
            "            \"serviceId\": \"238\",\n" +
            "            \"frequency\": \"610\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"游戏竞技卫视\",\n" +
            "        \"id\": \"138\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"92\",\n" +
            "            \"serviceId\": \"254\",\n" +
            "            \"frequency\": \"443\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"中国交通卫视\",\n" +
            "        \"id\": \"1391\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"95\",\n" +
            "            \"serviceId\": \"150\",\n" +
            "            \"frequency\": \"610\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"美食天府卫视\",\n" +
            "        \"id\": \"140\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"98\",\n" +
            "            \"serviceId\": \"236\",\n" +
            "            \"frequency\": \"650\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"教育.就业卫视\",\n" +
            "        \"id\": \"1411\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"100\",\n" +
            "            \"serviceId\": \"115\",\n" +
            "            \"frequency\": \"634\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"四海钓鱼卫视\",\n" +
            "        \"id\": \"118\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"101\",\n" +
            "            \"serviceId\": \"118\",\n" +
            "            \"frequency\": \"634\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"动感音乐卫视\",\n" +
            "        \"id\": \"1431\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"102\",\n" +
            "            \"serviceId\": \"112\",\n" +
            "            \"frequency\": \"634\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"车迷卫视\",\n" +
            "        \"id\": \"1441\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"103\",\n" +
            "            \"serviceId\": \"116\",\n" +
            "            \"frequency\": \"554\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"新娱乐卫视\",\n" +
            "        \"id\": \"1451\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"104\",\n" +
            "            \"serviceId\": \"191\",\n" +
            "            \"frequency\": \"554\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"环球旅游卫视\",\n" +
            "        \"id\": \"146\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"105\",\n" +
            "            \"serviceId\": \"119\",\n" +
            "            \"frequency\": \"554\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"收藏天下卫视\",\n" +
            "        \"id\": \"147\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"111\",\n" +
            "            \"serviceId\": \"205\",\n" +
            "            \"frequency\": \"690\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"快乐宠物卫视\",\n" +
            "        \"id\": \"148\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"112\",\n" +
            "            \"serviceId\": \"192\",\n" +
            "            \"frequency\": \"690\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"中国气象卫视\",\n" +
            "        \"id\": \"1491\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"121\",\n" +
            "            \"serviceId\": \"241\",\n" +
            "            \"frequency\": \"610\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"京视剧场卫视\",\n" +
            "        \"id\": \"150\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"134\",\n" +
            "            \"serviceId\": \"111\",\n" +
            "            \"frequency\": \"554\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"置业卫视\",\n" +
            "        \"id\": \"1511\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"135\",\n" +
            "            \"serviceId\": \"120\",\n" +
            "            \"frequency\": \"554\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"东方财经卫视\",\n" +
            "        \"id\": \"152\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"136\",\n" +
            "            \"serviceId\": \"215\",\n" +
            "            \"frequency\": \"570\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"全纪实卫视\",\n" +
            "        \"id\": \"1531\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"137\",\n" +
            "            \"serviceId\": \"216\",\n" +
            "            \"frequency\": \"387\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"魅力音乐卫视\",\n" +
            "        \"id\": \"1541\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"138\",\n" +
            "            \"serviceId\": \"220\",\n" +
            "            \"frequency\": \"387\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"生活时尚卫视\",\n" +
            "        \"id\": \"155\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"139\",\n" +
            "            \"serviceId\": \"221\",\n" +
            "            \"frequency\": \"387\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"七彩戏剧卫视\",\n" +
            "        \"id\": \"156\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"140\",\n" +
            "            \"serviceId\": \"223\",\n" +
            "            \"frequency\": \"570\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"劲爆体育卫视\",\n" +
            "        \"id\": \"157\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"141\",\n" +
            "            \"serviceId\": \"217\",\n" +
            "            \"frequency\": \"387\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"游戏风云卫视\",\n" +
            "        \"id\": \"158\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"142\",\n" +
            "            \"serviceId\": \"218\",\n" +
            "            \"frequency\": \"387\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"动漫秀场卫视\",\n" +
            "        \"id\": \"159\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"143\",\n" +
            "            \"serviceId\": \"219\",\n" +
            "            \"frequency\": \"387\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"极速汽车卫视\",\n" +
            "        \"id\": \"160\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"144\",\n" +
            "            \"serviceId\": \"224\",\n" +
            "            \"frequency\": \"387\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"法治天地卫视\",\n" +
            "        \"id\": \"1611\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"145\",\n" +
            "            \"serviceId\": \"225\",\n" +
            "            \"frequency\": \"570\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"欢笑剧场卫视\",\n" +
            "        \"id\": \"1621\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"146\",\n" +
            "            \"serviceId\": \"226\",\n" +
            "            \"frequency\": \"387\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"金色频道卫视\",\n" +
            "        \"id\": \"163\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"147\",\n" +
            "            \"serviceId\": \"227\",\n" +
            "            \"frequency\": \"570\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CHC动作电影卫视\",\n" +
            "        \"id\": \"149\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"151\",\n" +
            "            \"serviceId\": \"149\",\n" +
            "            \"frequency\": \"610\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"网络棋牌卫视\",\n" +
            "        \"id\": \"165\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"152\",\n" +
            "            \"serviceId\": \"234\",\n" +
            "            \"frequency\": \"610\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"中华美食卫视\",\n" +
            "        \"id\": \"1661\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"153\",\n" +
            "            \"serviceId\": \"242\",\n" +
            "            \"frequency\": \"610\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"CHC家庭影院卫视\",\n" +
            "        \"id\": \"821\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"161\",\n" +
            "            \"serviceId\": \"148\",\n" +
            "            \"frequency\": \"610\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"都市剧场卫视\",\n" +
            "        \"id\": \"1681\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"162\",\n" +
            "            \"serviceId\": \"222\",\n" +
            "            \"frequency\": \"387\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"优优宝贝卫视\",\n" +
            "        \"id\": \"169\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"163\",\n" +
            "            \"serviceId\": \"117\",\n" +
            "            \"frequency\": \"634\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"弈坛春秋卫视\",\n" +
            "        \"id\": \"1701\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"165\",\n" +
            "            \"serviceId\": \"114\",\n" +
            "            \"frequency\": \"554\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"高尔夫.网球卫视\",\n" +
            "        \"id\": \"232\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"170\",\n" +
            "            \"serviceId\": \"136\",\n" +
            "            \"frequency\": \"674\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"老故事卫视\",\n" +
            "        \"id\": \"240\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"171\",\n" +
            "            \"serviceId\": \"240\",\n" +
            "            \"frequency\": \"690\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"书画卫视\",\n" +
            "        \"id\": \"1731\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"173\",\n" +
            "            \"serviceId\": \"243\",\n" +
            "            \"frequency\": \"690\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"第一剧场卫视\",\n" +
            "        \"id\": \"822\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"201\",\n" +
            "            \"serviceId\": \"137\",\n" +
            "            \"frequency\": \"674\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"电视指南卫视\",\n" +
            "        \"id\": \"175\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"202\",\n" +
            "            \"serviceId\": \"138\",\n" +
            "            \"frequency\": \"674\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"风云剧场卫视\",\n" +
            "        \"id\": \"139\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"203\",\n" +
            "            \"serviceId\": \"139\",\n" +
            "            \"frequency\": \"674\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"风云音乐卫视\",\n" +
            "        \"id\": \"1771\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"204\",\n" +
            "            \"serviceId\": \"140\",\n" +
            "            \"frequency\": \"674\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"风云足球卫视\",\n" +
            "        \"id\": \"141\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"205\",\n" +
            "            \"serviceId\": \"141\",\n" +
            "            \"frequency\": \"674\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"怀旧剧场卫视\",\n" +
            "        \"id\": \"142\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"206\",\n" +
            "            \"serviceId\": \"142\",\n" +
            "            \"frequency\": \"674\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"央视精品卫视\",\n" +
            "        \"id\": \"1801\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"207\",\n" +
            "            \"serviceId\": \"143\",\n" +
            "            \"frequency\": \"674\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"世界地理卫视\",\n" +
            "        \"id\": \"144\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"208\",\n" +
            "            \"serviceId\": \"144\",\n" +
            "            \"frequency\": \"674\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"国防军事卫视\",\n" +
            "        \"id\": \"824\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"209\",\n" +
            "            \"serviceId\": \"145\",\n" +
            "            \"frequency\": \"674\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"女性时尚卫视\",\n" +
            "        \"id\": \"1831\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"210\",\n" +
            "            \"serviceId\": \"146\",\n" +
            "            \"frequency\": \"690\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"发现之旅卫视\",\n" +
            "        \"id\": \"260\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"211\",\n" +
            "            \"serviceId\": \"260\",\n" +
            "            \"frequency\": \"690\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"财富天下卫视\",\n" +
            "        \"id\": \"314\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"212\",\n" +
            "            \"serviceId\": \"314\",\n" +
            "            \"frequency\": \"443\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"现代女性卫视\",\n" +
            "        \"id\": \"1861\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"213\",\n" +
            "            \"serviceId\": \"262\",\n" +
            "            \"frequency\": \"690\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"环球奇观卫视\",\n" +
            "        \"id\": \"1871\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"214\",\n" +
            "            \"serviceId\": \"263\",\n" +
            "            \"frequency\": \"690\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"靓妆卫视\",\n" +
            "        \"id\": \"1881\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"219\",\n" +
            "            \"serviceId\": \"250\",\n" +
            "            \"frequency\": \"443\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"天元围棋卫视\",\n" +
            "        \"id\": \"253\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"222\",\n" +
            "            \"serviceId\": \"253\",\n" +
            "            \"frequency\": \"443\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"篮球卫视\",\n" +
            "        \"id\": \"1901\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"224\",\n" +
            "            \"serviceId\": \"255\",\n" +
            "            \"frequency\": \"610\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"茶频道卫视\",\n" +
            "        \"id\": \"191\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"227\",\n" +
            "            \"serviceId\": \"258\",\n" +
            "            \"frequency\": \"443\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      {\n" +
            "        \"channelName\": \"3D卫视高清\",\n" +
            "        \"id\": \"192\",\n" +
            "        \"params\": {\n" +
            "          \"EXT_PARAM\": {\n" +
            "            \"bouquetId\": \"7\",\n" +
            "            \"tsID\": \"701\",\n" +
            "            \"serviceId\": \"561\",\n" +
            "            \"frequency\": \"379\"\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    ],\n" +
            "    \"packName\": \"com.bgctv.live\"\n" +
            "  },\n" +
            "  \"retryConfigInterval\": 7200\n" +
            "}";
}
