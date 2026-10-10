package com.nosso.cantinho;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@CapacitorPlugin(name = "ApkInstaller")
public class ApkInstallerPlugin extends Plugin {

    @PluginMethod
    public void canInstall(PluginCall call) {
        JSObject r = new JSObject();
        boolean ok = Build.VERSION.SDK_INT < 26 || getContext().getPackageManager().canRequestPackageInstalls();
        r.put("value", ok);
        call.resolve(r);
    }

    @PluginMethod
    public void openSettings(PluginCall call) {
        Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + getContext().getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
        call.resolve();
    }

    @PluginMethod
    public void install(final PluginCall call) {
        final String url = call.getString("url");
        if (url == null || url.isEmpty()) {
            call.reject("url ausente");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = new File(getContext().getCacheDir(), "apk");
                    dir.mkdirs();
                    File out = new File(dir, "update.apk");
                    if (out.exists()) out.delete();

                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setInstanceFollowRedirects(true);
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(30000);
                    int code = c.getResponseCode();
                    if (code != 200) {
                        call.reject("servidor respondeu " + code);
                        return;
                    }
                    long total = c.getContentLengthLong();
                    InputStream in = c.getInputStream();
                    FileOutputStream fo = new FileOutputStream(out);
                    byte[] buf = new byte[16384];
                    int n;
                    long done = 0;
                    int last = -1;
                    while ((n = in.read(buf)) > 0) {
                        fo.write(buf, 0, n);
                        done += n;
                        if (total > 0) {
                            int pct = (int) (done * 100 / total);
                            if (pct != last) {
                                last = pct;
                                JSObject p = new JSObject();
                                p.put("percent", pct);
                                notifyListeners("progress", p);
                            }
                        }
                    }
                    fo.close();
                    in.close();

                    if (total > 0 && out.length() != total) {
                        call.reject("download incompleto (" + out.length() + " de " + total + " bytes)");
                        return;
                    }
                    android.content.pm.PackageManager pm = getContext().getPackageManager();
                    String me = getContext().getPackageName();
                    int flags = Build.VERSION.SDK_INT >= 28
                            ? android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                            : android.content.pm.PackageManager.GET_SIGNATURES;
                    android.content.pm.PackageInfo np = pm.getPackageArchiveInfo(out.getAbsolutePath(), flags);
                    if (np == null) {
                        call.reject("o arquivo baixado nao e um APK valido");
                        return;
                    }
                    if (!me.equals(np.packageName)) {
                        call.reject("APK de outro app: " + np.packageName);
                        return;
                    }
                    android.content.pm.PackageInfo cur = pm.getPackageInfo(me, flags);
                    long nv = Build.VERSION.SDK_INT >= 28 ? np.getLongVersionCode() : np.versionCode;
                    long cv = Build.VERSION.SDK_INT >= 28 ? cur.getLongVersionCode() : cur.versionCode;
                    if (nv <= cv) {
                        call.reject("a versao baixada (" + nv + ") nao e mais nova que a instalada (" + cv + ")");
                        return;
                    }
                    if (!sameSigner(np, cur)) {
                        call.reject("ASSINATURA DIFERENTE: a versao instalada foi assinada com outra chave. Desinstale o app uma vez e instale o APK novo (os dados ficam na nuvem).");
                        return;
                    }
                    installSession(out, call);
                    call.resolve();
                } catch (Exception e) {
                    call.reject("falha: " + e.getMessage());
                }
            }
        }).start();
    }

    private boolean sameSigner(android.content.pm.PackageInfo a, android.content.pm.PackageInfo b) {
        try {
            android.content.pm.Signature[] x, y;
            if (Build.VERSION.SDK_INT >= 28) {
                x = a.signingInfo.getApkContentsSigners();
                y = b.signingInfo.getApkContentsSigners();
            } else {
                x = a.signatures;
                y = b.signatures;
            }
            if (x == null || y == null) return true;
            for (android.content.pm.Signature s1 : x)
                for (android.content.pm.Signature s2 : y)
                    if (s1.equals(s2)) return true;
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private void installSession(File apk, final PluginCall call) throws Exception {
        final android.content.Context ctx = getContext();
        final String action = ctx.getPackageName() + ".INSTALL_RESULT";
        android.content.pm.PackageInstaller pi = ctx.getPackageManager().getPackageInstaller();
        android.content.pm.PackageInstaller.SessionParams sp = new android.content.pm.PackageInstaller.SessionParams(
                android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        sp.setAppPackageName(ctx.getPackageName());
        int id = pi.createSession(sp);
        android.content.pm.PackageInstaller.Session ses = pi.openSession(id);
        java.io.InputStream in = new java.io.FileInputStream(apk);
        java.io.OutputStream os = ses.openWrite("update", 0, apk.length());
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
        ses.fsync(os);
        os.close();
        in.close();
        android.content.BroadcastReceiver rc = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context c, Intent it) {
                int st = it.getIntExtra(android.content.pm.PackageInstaller.EXTRA_STATUS, -999);
                if (st == android.content.pm.PackageInstaller.STATUS_PENDING_USER_ACTION) {
                    Intent confirm = (Intent) it.getParcelableExtra(Intent.EXTRA_INTENT);
                    if (confirm != null) {
                        confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        c.startActivity(confirm);
                    }
                    return;
                }
                try { c.unregisterReceiver(this); } catch (Exception ignore) {}
                if (st == android.content.pm.PackageInstaller.STATUS_SUCCESS) {
                    JSObject r = new JSObject();
                    r.put("status", "ok");
                    notifyListeners("result", r);
                } else {
                    String msg = it.getStringExtra(android.content.pm.PackageInstaller.EXTRA_STATUS_MESSAGE);
                    JSObject r = new JSObject();
                    r.put("status", "erro");
                    r.put("code", st);
                    r.put("message", msg == null ? "" : msg);
                    notifyListeners("result", r);
                }
            }
        };
        android.content.IntentFilter f = new android.content.IntentFilter(action);
        if (Build.VERSION.SDK_INT >= 33) ctx.registerReceiver(rc, f, android.content.Context.RECEIVER_EXPORTED);
        else ctx.registerReceiver(rc, f);
        Intent cb = new Intent(action).setPackage(ctx.getPackageName());
        int pf = android.app.PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 31) pf |= android.app.PendingIntent.FLAG_MUTABLE;
        android.app.PendingIntent pend = android.app.PendingIntent.getBroadcast(ctx, id, cb, pf);
        ses.commit(pend.getIntentSender());
        ses.close();
    }

    @PluginMethod
    public void addToCalendar(PluginCall call) {
        try {
            long start = (long) call.getDouble("start", 0.0).doubleValue();
            long end = (long) call.getDouble("end", (double) (start + 3600000L)).doubleValue();
            Intent i = new Intent(Intent.ACTION_INSERT);
            i.setData(CalendarContract.Events.CONTENT_URI);
            i.putExtra(CalendarContract.Events.TITLE, call.getString("title", "Nosso Cantinho"));
            i.putExtra(CalendarContract.Events.DESCRIPTION, call.getString("desc", ""));
            i.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start);
            i.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end);
            i.putExtra(CalendarContract.Events.HAS_ALARM, 1);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject("Nao consegui abrir a agenda: " + e.getMessage());
        }
    }
}
