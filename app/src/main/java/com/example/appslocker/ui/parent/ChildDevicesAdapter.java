package com.example.appslocker.ui.parent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appslocker.R;
import com.example.appslocker.data.model.ChildDevice;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChildDevicesAdapter extends RecyclerView.Adapter<ChildDevicesAdapter.ChildDeviceViewHolder> {

    private List<ChildDevice> childDevices;
    private OnChildDeviceClickListener listener;

    public interface OnChildDeviceClickListener {
        void onDeviceClick(ChildDevice device);
        void onManageClick(ChildDevice device);
    }

    public ChildDevicesAdapter(List<ChildDevice> childDevices, OnChildDeviceClickListener listener) {
        this.childDevices = childDevices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChildDeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_child_device, parent, false);
        return new ChildDeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildDeviceViewHolder holder, int position) {
        ChildDevice device = childDevices.get(position);
        holder.bind(device, listener);
    }

    @Override
    public int getItemCount() {
        return childDevices.size();
    }

    public void updateDevices(List<ChildDevice> newDevices) {
        childDevices.clear();
        childDevices.addAll(newDevices);
        notifyDataSetChanged();
    }

    static class ChildDeviceViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDeviceName, tvDeviceStatus, tvBlockedApps;
        private ImageView ivDeviceIcon, ivOnlineStatus;
        private View btnManage;

        public ChildDeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            tvDeviceStatus = itemView.findViewById(R.id.tv_device_status);
            tvBlockedApps = itemView.findViewById(R.id.tv_blocked_apps);
            ivDeviceIcon = itemView.findViewById(R.id.iv_device_icon);
            ivOnlineStatus = itemView.findViewById(R.id.iv_online_status);
            btnManage = itemView.findViewById(R.id.btn_manage);
        }

        public void bind(ChildDevice device, OnChildDeviceClickListener listener) {
            tvDeviceName.setText(device.getDeviceName());

            // Статус устройства
            if (device.isOnline()) {
                tvDeviceStatus.setText("В сети");
                tvDeviceStatus.setTextColor(itemView.getContext().getColor(R.color.green));
                ivOnlineStatus.setImageResource(R.drawable.ic_connected);
            } else {
                tvDeviceStatus.setText("Не в сети");
                tvDeviceStatus.setTextColor(itemView.getContext().getColor(R.color.gray));
                ivOnlineStatus.setImageResource(R.drawable.ic_disconnected);
            }

            // Заблокированные приложения - используем getBlockedAppsCount()
            int blockedCount = device.getBlockedAppsCount();
            tvBlockedApps.setText("Заблокировано приложений: " + blockedCount);

            // Обработчики кликов
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceClick(device);
                }
            });

            btnManage.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onManageClick(device);
                }
            });
        }
    }
}