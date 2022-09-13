package comp5216.sydney.edu.au.mediarecording.entity;

public class MediaInfo {

    private  long id;

    private String documentId;

    private String url;

    private  String city;

    private  Integer type;

    private  double syncStatus;


    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public double getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(double syncStatus) {
        this.syncStatus = syncStatus;
    }
}
