package vortispy.doya;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.test.ApplicationTestCase;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
 * Created by vortispy on 2014/10/05.
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    public ApplicationTest() {
        super(Application.class);
    }

    public void testBitmapResize() {
        int height = 48, width = 48, minwh;
        Context context = getContext();
        assertNotNull(context);
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.dog2);
        minwh = minmum(bitmap.getWidth(), bitmap.getHeight());
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, minwh, minwh);
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, height, width, false);

        assertEquals(height, resized.getHeight());
        assertEquals(width, resized.getWidth());
        saveBitmapToSd(resized);
    }

    public void testReduceBitmap(){
        Bitmap bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.dog2);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int preSize = bitmap.getByteCount();

        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
    }

    public int minmum(int a, int b){
        if(a <= b){
            return a;
        }
        return b;
    }

    public void saveBitmapToSd(Bitmap mBitmap) {
        try {
            // sdcardフォルダを指定
            File root = new File(Environment.getExternalStorageDirectory().getPath() + "/Pictures");
            Log.d("hoge", root.toString());

            // 日付でファイル名を作成　
            Date mDate = new Date();
            SimpleDateFormat fileName = new SimpleDateFormat("yyyyMMdd_HHmmss");

            // 保存処理開始
            FileOutputStream fos = null;
            fos = new FileOutputStream(new File(root, fileName.format(mDate) + ".jpg"));

            // jpegで保存
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            // 保存処理終了
            fos.close();
        } catch (Exception e) {
            Log.e("Error", "" + e.toString());
        }
    }
}
