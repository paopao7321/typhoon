package com.example.gddemo.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.OnInfoWindowClickListener;
import com.amap.api.maps.AMap.OnMapClickListener;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.example.gddemo.db.MyDAO;
import com.example.gddemo.db.DBOpenHelper.MODE;
import com.example.gddemo.R;

public class showTyphoonActivity extends Activity {
	private MapView mapView;
	private AMap aMap;
	private List<TyphoonPoint[]> tList = new ArrayList<TyphoonPoint[]>();
	private Map<LatLng, Marker> map = new HashMap<LatLng, Marker>();
	private MarkerOptions pointOptions;
	private static float MIN_DISTANCE = 500000f;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_show_typhoon);
		mapView = (MapView) findViewById(R.id.map);
		mapView.onCreate(savedInstanceState);// ����Ҫд
		init();

		pointOptions = new MarkerOptions()
				.icon(BitmapDescriptorFactory
						.fromResource(R.drawable.icon_bullet_yellow))
				.anchor(0.5f, 0.5f).perspective(true);

		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				getTYData();

				// ���ó�ʼ��ͼ���ĵ�����ż���
				TyphoonPoint[] ps = tList.get(0);
				double latitude = ((ps[0].getLatitude() + ps[ps.length - 1]
						.getLatitude()) / 2 + ps[ps.length / 2].getLatitude()) / 2;
				double longitude = ((ps[0].getLongitude() + ps[ps.length - 1]
						.getLongitude()) / 2 + ps[ps.length / 2].getLongitude()) / 2;
				aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(
						latitude, longitude)));
				aMap.moveCamera(CameraUpdateFactory.zoomTo(6));

				// ���ص㣬���ߣ�����

				for (TyphoonPoint[] tps : tList) {
					List<LatLng> pts = new ArrayList<LatLng>();

					for (int i = 0; i < tps.length; i++) {

						String wnd = tps[i].getWND();
						if (wnd.equals("0")) {
							wnd = "ȱ��\n";
						} else if (wnd.equals("9")) {
							wnd = "< 10m/s\n";
						} else {
							wnd += "m/s\n";
						}

						// ���õ��Marker��־���InfoWindow��Ϣ���� �����ؽ���ͼ

						Marker point = aMap.addMarker(pointOptions.position(
								tps[i].getPoint()).title(
								"		γ�ȣ� " + tps[i].getLatitude() + "��N\n"
										+ "		���ȣ�" + tps[i].getLongitude()

										+ "��E\n" + "		����ǿ�ȣ�"
										+ tps[i].getLevel() + "\n" + "		��������"
										+ wnd + "		�����ѹ��" + tps[i].getPRES()
										+ "hpa\n" + "		ʱ�䣺" + tps[i].getTime()));
						map.put(tps[i].getPoint(), point);

						// ��ӵ��������������ߵ�List
						pts.add(tps[i].getPoint());

					}

					// �������߲����ؽ���ͼ
					PolylineOptions polyline = new PolylineOptions().width(6)
							.addAll(pts).setDottedLine(true).color(0xFFE23A00);
					aMap.addPolyline(polyline);
				}

			}

		}).start();

		aMap.setOnMapClickListener(new OnMapClickListener() {

			@Override
			public void onMapClick(LatLng arg0) {
				// TODO Auto-generated method stub
				Map minMap = new HashMap<String, Marker>();
				float length;
				float min = Float.MAX_VALUE;
				Marker mMarker = null;
				Set<LatLng> set = map.keySet();
				for (Iterator<LatLng> it = set.iterator(); it.hasNext();) {
					LatLng latlng = it.next();
					if ((length = AMapUtils.calculateLineDistance(latlng, arg0)) <= MIN_DISTANCE) {
						if (length < min) {
							min = length;
							mMarker = (Marker) map.get(latlng);
						}
					}
				}

				if (mMarker != null) {

					mMarker.showInfoWindow();
				}
			}

		});

		aMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

			@Override
			public void onInfoWindowClick(Marker mMarker) {
				// TODO Auto-generated method stub
				if (mMarker.isInfoWindowShown()) {
					mMarker.hideInfoWindow();
				}
			}
		});

	}

	/**
	 * ��ʼ��AMap����
	 */
	private void init() {
		if (aMap == null) {
			aMap = mapView.getMap();
			// ��ʾ������
			aMap.getUiSettings().setScaleControlsEnabled(true);
			// ���õ�ͼ���ĵ�
			aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(23.0,
					130.0)));
			// �������ű���
			aMap.moveCamera(CameraUpdateFactory.zoomTo(6));

		}
	}

	/**
	 * ����������д
	 */
	@Override
	protected void onResume() {
		super.onResume();
		mapView.onResume();
	}

	/**
	 * ����������д
	 */
	@Override
	protected void onPause() {
		super.onPause();
		mapView.onPause();
	}

	/**
	 * ����������д
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	/**
	 * ����������д
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
	}

	private void getTYData() {
		TyphoonPoint[] tpoint = null;
		MyDAO daoSet = new MyDAO(this, "typhoonSet.db", MODE.MODE_TYPHOONSet);
		MyDAO daoTY = new MyDAO(this, "typhoon.db", MODE.MODE_TYPHOONSet);
		try {
			Intent intent = getIntent();
			String name = intent.getStringExtra("typhoonName");

			int year = 0, length = 0, subCenterCount = 0;
			String date = intent.getStringExtra("date");
			String id = intent.getStringExtra("id");
			SQLiteDatabase set = daoSet.getReadableDB();
			Cursor cursor = null, c = null, csub = null;
			set.beginTransaction();
			{
				try {
					cursor = set
							.rawQuery(
									"select year,subCenterCount,length from typhoonSet where name=? and year = ? and id = ?",
									new String[] { name, date.substring(0, 4),
											id });
					set.setTransactionSuccessful();
				} finally {
					set.endTransaction();
				}
			}
			SQLiteDatabase ty = daoTY.getReadableDB();
			if (cursor.moveToNext()) {
				year = cursor.getInt(0);
				subCenterCount = cursor.getInt(1);
				length = cursor.getInt(2);

				tpoint = new TyphoonPoint[length];
				int i = 0;

				c = ty.rawQuery(
						"select time,level,lat,lon,pres,wnd	 from typhoon where name = ? and id=? and year = ?",
						new String[] { name, id, Integer.toString(year) });
				while (c.moveToNext()) {
					String time = c.getString(0);
					int level = c.getInt(1);
					int lat = c.getInt(2);
					int lon = c.getInt(3);
					int pres = c.getInt(4);
					int wnd = c.getInt(5);

					tpoint[i++] = new TyphoonPoint((float) (lat / 10.0),
							(float) (lon / 10.0), pres, wnd, level, time);

				}
				tList.add(tpoint);

				if (subCenterCount > 0) {

					for (int k = 1; k <= subCenterCount; k++) {

						String n = name + "(-)" + k;
						int subLength = 0;
						set.beginTransaction();
						try {
							csub = set
									.rawQuery(
											"select length from typhoonSet where name = ? and id=? and year = ?",
											new String[] { n, id,
													Integer.toString(year) });
						} finally {
							set.endTransaction();
						}
						csub.moveToNext();
						subLength = csub.getInt(0);
						ty.beginTransaction();
						try {
							csub = ty
									.rawQuery(
											"select time,level,lat,lon,pres,wnd	 from typhoon where name = ? and id=? and year = ?",
											new String[] { n, id,
													Integer.toString(year) });
						} finally {
							ty.endTransaction();
						}
						tpoint = new TyphoonPoint[subLength];
						int m = 0;
						while (csub.moveToNext()) {
							String time = csub.getString(0);
							int level = csub.getInt(1);
							int lat = csub.getInt(2);
							int lon = csub.getInt(3);
							int pres = csub.getInt(4);
							int wnd = csub.getInt(5);

							tpoint[m++] = new TyphoonPoint(
									(float) (lat / 10.0), (float) (lon / 10.0),
									pres, wnd, level, time);
						}
						tList.add(tpoint);
					}
				}

			}
		} finally {
			if (daoSet != null)
				daoSet.close();
			if (daoTY != null)
				daoTY.close();
		}

	}

}

class TyphoonPoint {
	private String level;
	private float lat, lon;
	private String pres, wnd, time;

	public TyphoonPoint(float lat, float lon, int pres, int wnd, int level,
			String date) {
		this.lat = lat;
		this.lon = lon;
		this.pres = Integer.toString(pres);
		this.wnd = Integer.toString(wnd);
		this.time = date;
		switch (level) {
		case 0:
			this.level = "�����ȴ���ѹ(TD)";
			break;
		case 1:
			this.level = "�ȴ���ѹ(TD)";
			break;
		case 2:
			this.level = "�ȴ��籩(TS)";
			break;
		case 3:
			this.level = "ǿ�ȴ��籩(STS)";
			break;
		case 4:
			this.level = "̨��(TY)";
			break;
		case 5:
			this.level = "ǿ̨��(STY)";
			break;
		case 6:
			this.level = "��ǿ̨��(SuperTY)";
			break;
		case 9:
			this.level = "����";
			break;
		}

	}

	public float getLatitude() {

		return lat;

	}

	public float getLongitude() {
		return lon;
	}

	public String getPRES() {
		return pres;
	}

	public String getWND() {
		return wnd;
	}

	public LatLng getPoint() {
		return new LatLng(lat, lon);
	}

	public String getLevel() {
		return level;
	}

	public String getTime() {
		return time.substring(0, 4) + "/" + time.substring(5, 6) + "/"
				+ time.substring(6, 8) + " " + time.substring(8) + ":00";

	}

}
