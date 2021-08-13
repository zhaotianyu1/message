package com.juphoon.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.messaging.R;


public class JuphoonStyleDialog {
   public interface INegativeListener {
        void onClick(View v);
    }

    public interface IPositiveListener {
        void onClick(View v);
    }

    public interface IDismissListener {
        void onDismiss(DialogInterface dialog);
    }


    public interface ICancelListener{
        void onCancel(DialogInterface dialog);
    }


    public static void showDialog(Context context, String titleText, String messageText, String negativeText, final INegativeListener negativeListener, String positiveText, final IPositiveListener positiveListener) {
        showDialog(context, titleText, messageText, null, negativeText, negativeListener, positiveText, positiveListener, null, null);
    }


    public static void showDialog(Context context, String titleText, String messageText, View view, String negativeText, final INegativeListener negativeListener, String positiveText, final IPositiveListener positiveListener) {
        showDialog(context, titleText, messageText, view, negativeText, negativeListener, positiveText, positiveListener, null, null);
    }


    public static void showDialog(Context context, String titleText, String messageText,
                                  View view,
                                  String negativeText, final JuphoonStyleDialog.INegativeListener negativeListener,
                                  String positiveText, final JuphoonStyleDialog.IPositiveListener positiveListener,
                                  final IDismissListener dismissListener,final ICancelListener cancelListener) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.juphoon_dialog_layout, null);
        Button btn_negative = v.findViewById(R.id.bt_negative);
        Button btn_positive = v.findViewById(R.id.bt_positive);
        if (!TextUtils.isEmpty(titleText)) {
            TextView title = v.findViewById(R.id.title);
            title.setText(titleText);
        }
        if (!TextUtils.isEmpty(messageText)) {
            TextView message = v.findViewById(R.id.message);
            message.setText(messageText);
            message.setVisibility(View.VISIBLE);
        }
        if (view != null) {
            RelativeLayout customView = v.findViewById(R.id.custom_view);
            customView.addView(view);
            customView.setVisibility(View.VISIBLE);
        }
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context, R.style.Juphoon_Dialog_style);
        if (dismissListener != null) {
            dialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    dismissListener.onDismiss(dialog);
                }
            });
        }
        if (cancelListener != null) {
            dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    cancelListener.onCancel(dialog);
                }
            });
        }
        final Dialog dialog = dialogBuilder.create();
        dialog.show();
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
        layoutParams.width = display.getWidth() / 4 * 3;
        dialog.getWindow().setAttributes(layoutParams);
        dialog.getWindow().setContentView(v);

        btn_negative.setText(negativeText);
        btn_positive.setText(positiveText);
        btn_negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (negativeListener != null) {
                    negativeListener.onClick(v);
                }
                dialog.dismiss();
            }
        });
        btn_positive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (positiveListener != null) {
                    positiveListener.onClick(v);
                }
                dialog.dismiss();
            }
        });
    }
}
