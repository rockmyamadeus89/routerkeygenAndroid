/*
 * Copyright 2012 Rui Araújo, Luís Fonseca
 *
 * This file is part of Router Keygen.
 *
 * Router Keygen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Router Keygen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Router Keygen.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.exobel.routerkeygen.algorithms;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.exobel.routerkeygen.R;

import android.os.Parcel;
import android.os.Parcelable;

public class BelkinKeygen extends Keygen {
	private final static int[][] ORDERS = { { 6, 2, 3, 8, 5, 1, 7, 4 },
			{ 1, 2, 3, 8, 5, 1, 7, 4 }, { 1, 2, 3, 8, 5, 6, 7, 4 } };
	private final static String[] CHARSETS = { "024613578ACE9BDF",
			"944626378ace9bdf" };
	protected MessageDigest md;

	public BelkinKeygen(String ssid, String mac) {
		super(ssid, mac);
	}

	@Override
	public int getSupportState() {
		if (getSsidName().matches("^(B|b)elkin(\\.|_)[0-9a-fA-F]{3,6}$"))
			return SUPPORTED;
		return UNLIKELY_SUPPORTED;
	}
	
	protected void generateKey(String mac, String charset, int [] order) {
		StringBuilder key = new StringBuilder();
	    if ( mac.length() != 8 ) {
	        return;
	    }
	    for ( int i = 0; i < mac.length(); ++i ){
	        String k = mac.substring(order[i]-1, order[i]);
	        key.append(charset.charAt(Integer.parseInt(k, 16)));
	    }
		addPassword(key.toString());
	}

	@Override
	public List<String> getKeys() {
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e1) {
			setErrorCode(R.string.msg_nosha256);
			return null;
		}
		String mac = getMacAddress();
		if (mac.length() != 12) {
			setErrorCode(R.string.msg_nomac);
			return null;
		}
	    String ssid = getSsidName();
	    if (ssid.startsWith("B")) {
	        generateKey(mac.substring(4), CHARSETS[0], ORDERS[0]);
	    } else if (ssid.startsWith("b")) {
	        mac = incrementMac(mac, 1);
	        generateKey(mac.substring(4), CHARSETS[1], ORDERS[0]);
	        if (!mac.startsWith("944452")) {
	            generateKey(mac.substring(4), CHARSETS[1], ORDERS[2]);
		        mac = incrementMac(mac, 1);
	            generateKey(mac.substring(4), CHARSETS[1], ORDERS[0]);
	        }
	    } else {
	        //Bruteforcing
	        for (int i = 0; i < 3; ++i) {
	            for ( int j = 0; j < ORDERS.length; ++j ) {
	                generateKey(mac.substring(4), CHARSETS[0], ORDERS[j]);
	                generateKey(mac.substring(4), CHARSETS[1], ORDERS[j]);
	            }
		        mac = incrementMac(mac, 1);
	        }
	    }
		return getResults();
	}

	protected BelkinKeygen(Parcel in) {
		super(in);
	}

	public static final Parcelable.Creator<BelkinKeygen> CREATOR = new Parcelable.Creator<BelkinKeygen>() {
		public BelkinKeygen createFromParcel(Parcel in) {
			return new BelkinKeygen(in);
		}

		public BelkinKeygen[] newArray(int size) {
			return new BelkinKeygen[size];
		}
	};

}
