package vortispy.doya;

import android.graphics.Bitmap;

/**
 * Created by vortispy on 2014/07/19.
 */
public class DoyaData {
    private Bitmap imageData;
    private String objectKey;
    private Integer doyaPoint;
    private Integer doyaRank;

    public void setDoyaPoint(Integer doyaPoint) {
        this.doyaPoint = doyaPoint;
    }

    public void setImageData(Bitmap imageData) {
        this.imageData = imageData;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public void setDoyaRank(Integer doyaRank) {
        this.doyaRank = doyaRank;
    }

    public Bitmap getImageData() {
        return imageData;
    }

    public Integer getDoyaPoint() {
        return doyaPoint;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public Integer getDoyaRank() {
        return doyaRank;
    }
}
