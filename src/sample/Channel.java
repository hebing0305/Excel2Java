package sample;

public class Channel {

  /**
   * ChannelID : 1
   * ChannelNumber : 001
   * ChannelName : CCTV1综合
   * ProgramName : 爸爸去哪儿了-100
   * movieURL : channel01.mp4
   * bouquetId : 1
   * frequency : 714
   * serviceId : 121
   * tsId : 1
   */

  private String ChannelID;
  private String ChannelName;
  //本地测试字段
  private String ChannelNumber;
  private String ProgramName;
  private String movieURL;
  //歌华需要字段
  private String bouquetId;
  private String frequency;
  private String serviceId;
  private String tsId;
  //广西南宁SID
  private String sid;

  public String getSid() {
    return sid;
  }

  public void setSid(String sid) {
    this.sid = sid;
  }

  public String getChannelID() {
    return ChannelID;
  }

  public void setChannelID(String ChannelID) {
    this.ChannelID = ChannelID;
  }

  public String getChannelNumber() {
    return ChannelNumber;
  }

  public void setChannelNumber(String ChannelNumber) {
    this.ChannelNumber = ChannelNumber;
  }

  public String getChannelName() {
    return ChannelName;
  }

  public void setChannelName(String ChannelName) {
    this.ChannelName = ChannelName;
  }

  public String getProgramName() {
    return ProgramName;
  }

  public void setProgramName(String ProgramName) {
    this.ProgramName = ProgramName;
  }

  public String getMovieURL() {
    return movieURL;
  }

  public void setMovieURL(String movieURL) {
    this.movieURL = movieURL;
  }

  public String getBouquetId() {
    return bouquetId;
  }

  public void setBouquetId(String bouquetId) {
    this.bouquetId = bouquetId;
  }

  public String getFrequency() {
    return frequency;
  }

  public void setFrequency(String frequency) {
    this.frequency = frequency;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getTsId() {
    return tsId;
  }

  public void setTsId(String tsId) {
    this.tsId = tsId;
  }

  @Override
  public String toString() {
    String str = "ChannelID:" + ChannelID + " ChannelName:" + ChannelName;
    return str;

  }
}
