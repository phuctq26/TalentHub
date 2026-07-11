-- SCRIPT KHỞI TẠO VÀ THÊM DỮ LIỆU NGƯỜI DÙNG (POSTGRESQL)
-- Phiên bản cập nhật sử dụng bảng 'role' riêng và liên kết 'role_id' qua khoá ngoại.

-- 1. Bật extension 'citext' để hỗ trợ lưu username & email không phân biệt chữ hoa/chữ thường (Case-insensitive)
CREATE EXTENSION IF NOT EXISTS citext;

-- 2. Tạo kiểu dữ liệu enum cho Trạng thái tài khoản (AccountStatus) nếu chưa tồn tại
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'account_status') THEN
        CREATE TYPE account_status AS ENUM ('ACTIVE', 'LOCKED', 'INACTIVE');
    END IF;
END$$;

-- 3. Tạo bảng 'role' (Bảng phân quyền mới) nếu chưa tồn tại
CREATE TABLE IF NOT EXISTS role (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- 4. Tạo bảng 'users' nếu chưa có, liên kết khoá ngoại 'role_id' tới bảng 'role'
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    username citext NOT NULL UNIQUE,
    email citext NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status account_status NOT NULL DEFAULT 'ACTIVE',
    phone VARCHAR(30),
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    deactivated_at TIMESTAMP WITH TIME ZONE,
    deactivated_by BIGINT REFERENCES users(id),
    role_id INT REFERENCES role(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 5. Nạp dữ liệu mẫu vào bảng 'role' (ADMIN, HR_MANAGER, INTERVIEWER, CANDIDATE)
-- Chúng ta gán cứng ID từ 1 đến 4 để đảm bảo tính nhất quán của dữ liệu mẫu.
INSERT INTO role (id, name)
VALUES 
    (1, 'ADMIN'),
    (2, 'HR_MANAGER'),
    (3, 'INTERVIEWER'),
    (4, 'CANDIDATE')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;

-- Đồng bộ lại Sequence của bảng 'role' để tránh lỗi tự tăng ID tiếp theo
SELECT setval(pg_get_serial_sequence('role', 'id'), COALESCE(MAX(id), 1)) FROM role;

-- 6. Chèn dữ liệu mẫu cho các người dùng (Sử dụng cột role_id liên kết bảng role)
-- Do chưa tích hợp Spring Security mã hóa mật khẩu, password_hash tạm thời được lưu dạng text hoặc mã hóa tùy ý.
INSERT INTO users (full_name, username, email, password_hash, role_id, status, phone, created_at, updated_at)
VALUES 
    (
        'Quản trị viên Hệ thống', 
        'admin', 
        'admin@talenthub.com', 
        'admin123', -- Mật khẩu thô (hoặc hash nếu sau này có BCrypt)
        1, -- ADMIN (role_id = 1)
        'ACTIVE', 
        '0912345678', 
        NOW(), 
        NOW()
    )
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (full_name, username, email, password_hash, role_id, status, phone, created_at, updated_at)
VALUES 
    (
        'Trưởng phòng Nhân sự', 
        'hrmanager', 
        'hr@talenthub.com', 
        'hr123', 
        2, -- HR_MANAGER (role_id = 2)
        'ACTIVE', 
        '0987654321', 
        NOW(), 
        NOW()
    )
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (full_name, username, email, password_hash, role_id, status, phone, created_at, updated_at)
VALUES 
    (
        'Người phỏng vấn kĩ thuật', 
        'interviewer', 
        'interviewer@talenthub.com', 
        'int123', 
        3, -- INTERVIEWER (role_id = 3)
        'ACTIVE', 
        '0909090909', 
        NOW(), 
        NOW()
    )
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (full_name, username, email, password_hash, role_id, status, phone, created_at, updated_at)
VALUES 
    (
        'Ứng viên Tiềm năng', 
        'candidate', 
        'candidate@talenthub.com', 
        'cand123', 
        4, -- CANDIDATE (role_id = 4)
        'ACTIVE', 
        '0955555555', 
        NOW(), 
        NOW()
    )
ON CONFLICT (username) DO NOTHING;

-- Kiểm tra kết quả sau khi chèn dữ liệu
SELECT u.id, u.full_name, u.username, u.email, r.name AS role_name, u.status, u.created_at 
FROM users u
LEFT JOIN role r ON u.role_id = r.id;
