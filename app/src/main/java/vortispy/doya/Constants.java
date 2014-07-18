/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package vortispy.doya;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;

import java.util.Locale;

public class Constants {
	//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	// This sample App is for demonstration purposes only.
	// It is not secure to embed your credentials into source code.
	// DO NOT EMBED YOUR CREDENTIALS IN PRODUCTION APPS.
	// We offer two solutions for getting credentials to your mobile App.
	// Please read the following article to learn about Token Vending Machine:
	// * http://aws.amazon.com/articles/Mobile/4611615499399490
	// Or consider using web identity federation:
	// * http://aws.amazon.com/articles/Mobile/4617974389850313
	//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	
//	public static final String ACCESS_KEY_ID = Resources.getSystem().getString(R.string.aws_access_key);
//	public static final String SECRET_KEY = Resources.getSystem().getString(R.string.aws_secret_key);

    public static String bucket = "Doya";
	public static String filename = "1.jpg";

    public static String ACCESS_KEY_ID;
    public static String SECRET_KEY;

    Constants(String access_key, String secret_key){
        this.ACCESS_KEY_ID = access_key;
        this.SECRET_KEY = secret_key;
    }

    public String setBucket(String bucket){
        this.bucket = bucket;
        return this.bucket;
    }

    public String setFileName(String filename){
        this.filename = filename;
        return this.filename;
    }
	
	public static String getBucket() {
		return (bucket).toLowerCase(Locale.US);
	}
	
}
