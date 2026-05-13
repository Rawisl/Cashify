package com.example.cashify.ui.main;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.data.model.Workspace;

import java.util.List;

public class WorkspaceAdapter extends RecyclerView.Adapter<WorkspaceAdapter.WorkspaceViewHolder>{
    //Layout Item: Bảo bạn dev UI thiết kế cái item_workspace_sidebar.xml nằm ngang. Bên trái là icon tròn, giữa là tên quỹ, dưới tên là số dư nhỏ nhỏ, bên phải có thể thêm một cái dấu tick nếu quỹ đó đang được chọn.
    //Sắp xếp: Nên để Quỹ Cá nhân luôn nằm ở vị trí đầu tiên (position 0) để user dễ tìm nhất.
    //Trạng thái trống: Nếu lỡ user chưa có quỹ nào (trường hợp hiếm), Side Bar nên hiện một dòng chữ "Thêm quỹ mới".
    private List<Workspace> workspaceList;
    private OnWorkspaceClickListener listener;
    private String selectedWorkspaceId = ""; // Dùng để highlight quỹ đang chọn

    // TODO 1: Khai báo Interface để MainActivity lắng nghe sự kiện click
    public interface OnWorkspaceClickListener {
        void onWorkspaceClick(Workspace workspace);
    }

    public WorkspaceAdapter(List<Workspace> workspaceList, OnWorkspaceClickListener listener) {
        this.workspaceList = workspaceList;
        this.listener = listener;
    }

    // TODO 2: Viết hàm cập nhật danh sách và Workspace đang được chọn (Selected ID)
    public void setWorkspaces(List<Workspace> workspaces, String currentId) {
        this.workspaceList = workspaces;
        this.selectedWorkspaceId = currentId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WorkspaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // TODO 3: Inflate layout item_workspace_sidebar.xml
        // View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workspace_sidebar, parent, false);
        // return new WorkspaceViewHolder(view);
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull WorkspaceViewHolder holder, int position) {
        Workspace workspace = workspaceList.get(position);

        // ============================================================
        // TODO 4: BIND DỮ LIỆU & LOGIC HIỂN THỊ
        // - Đổ tên Quỹ (workspace.getName()) và Số dư (workspace.getBalance()) vào TextView.
        // - LOGIC ICON:
        //      + IF (type == "PERSONAL") -> Hiện icon Người.
        //      + IF (type == "GROUP") -> Hiện icon Nhóm.
        // - LOGIC HIGHLIGHT:
        //      + IF (workspace.getId() == selectedWorkspaceId) -> Đổi màu nền/chữ để báo hiệu đây là quỹ đang xem.
        // ============================================================

        // ============================================================
        // TODO 5: XỬ LÝ CLICK
        // - Khi bấm vào item: Gọi listener.onWorkspaceClick(workspace).
        // ============================================================
    }

    @Override
    public int getItemCount() {
        return workspaceList != null ? workspaceList.size() : 0;
    }

    public static class WorkspaceViewHolder extends RecyclerView.ViewHolder {
        // TODO 6: Ánh xạ các View (Icon, Name, Balance, Container...) từ layout xml
        public WorkspaceViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
