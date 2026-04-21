package com.example.cashify.ui.workspace;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.jetbrains.annotations.Nullable;

public class InviteMemberBottomSheet extends BottomSheetDialogFragment {
    //Nơi chủ nhóm tạo lời mời gửi cho bạn bè vào chung Quỹ.
    //Tính năng "Tự động điền" (Autocomplete): Nếu ghệ muốn app xịn hơn, hãy nhắc bạn dev UI kết nối với danh sách bạn bè đã có (trong FriendsFragment). Khi gõ vài chữ cái, nó hiện ra gợi ý những người bạn của mình để mời cho nhanh, thay vì bắt user phải gõ nguyên cái email dài ngoằng.
    //UX Vuốt: BottomSheet có cái hay là user có thể vuốt xuống để hủy. Bảo bạn làm layout thêm một cái "handle" (vạch ngang nhỏ ở trên cùng) để user biết là cái này vuốt được.
    //Firebase Logic: Nhắc bạn làm Backend là khi mời thành viên, ngoài việc thêm UID vào mảng members, nên gửi một cái thông báo (Notification) cho người kia nữa nhé.
    private WorkspaceViewModel workspaceViewModel;
    private String currentWorkspaceId;

    // ============================================================
    // TODO 1: NHẬN WORKSPACE ID
    // - Dùng Bundle để truyền Workspace ID từ màn hình Detail hoặc Side Bar vào đây.
    // ============================================================
    public static InviteMemberBottomSheet newInstance(String workspaceId) {
        InviteMemberBottomSheet fragment = new InviteMemberBottomSheet();
        Bundle args = new Bundle();
        args.putString("workspace_id", workspaceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // TODO 2: return inflater.inflate(R.layout.bottom_sheet_invite_member, container, false);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            currentWorkspaceId = getArguments().getString("workspace_id");
        }

        initViewModel();
        initViews(view);
        observeViewModel();
    }

    private void initViewModel() {
        workspaceViewModel = new ViewModelProvider(this).get(WorkspaceViewModel.class);
    }

    private void initViews(View view) {
        // ============================================================
        // TODO 3: XỬ LÝ NÚT "GỬI LỜI MỜI"
        // - Lấy Email từ EditText nhập liệu.
        // - Kiểm tra Email hợp lệ.
        // - Gọi workspaceViewModel.inviteMember(currentWorkspaceId, email).
        // ============================================================
    }

    private void observeViewModel() {
        // ============================================================
        // TODO 4: QUAN SÁT KẾT QUẢ
        // - isLoading: Hiện thanh loading hoặc đổi nút thành trạng thái "đang gửi".
        // - isSuccess: Nếu TRUE -> Hiện Toast "Đã gửi lời mời thành công" và dismiss().
        // - errorMessage: Thông báo nếu Email không tồn tại hoặc người đó đã ở trong nhóm rồi.
        // ============================================================
    }
}
