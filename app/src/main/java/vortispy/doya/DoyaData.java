package vortispy.doya;

import android.graphics.Bitmap;

/**
 * Created by vortispy on 2014/07/19.
 */
public class DoyaData {
    private Bitmap imageData;
    private String objectKey;
    private Integer doyaPoint;

    public String url = "http://cdn-ak.f.st-hatena.com/images/fotolife/n/nobuoka/20130609/20130609001808.png";

    public void setDoyaPoint(Integer doyaPoint) {
        this.doyaPoint = doyaPoint;
    }

    public void setImageData(Bitmap imageData) {
        this.imageData = imageData;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
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
}
