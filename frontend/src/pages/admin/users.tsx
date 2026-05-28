import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import {
  App,
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { EnterpriseShell } from '../../components/enterprise-shell';
import { apiRequest } from '../../lib/api';
import { clearAuthSession, readAuthSession, type CurrentUser } from '../../lib/auth';

const { Title, Text } = Typography;

type Role = {
  id: number;
  code: string;
  name: string;
};

type UserListItem = {
  id: number;
  userCode: string;
  username: string;
  realName: string;
  status: 'ACTIVE' | 'DISABLED';
  createdAt: string;
  roles: Role[];
};

type CreateFormValues = {
  username: string;
  realName: string;
  nickname?: string;
  password: string;
  status: 'ACTIVE' | 'DISABLED';
  roleIds: number[];
};

type EditFormValues = {
  realName: string;
  nickname?: string;
  status: 'ACTIVE' | 'DISABLED';
  roleIds: number[];
};

export default function UserAdminPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState<string | undefined>();
  const [roles, setRoles] = useState<Role[]>([]);
  const [users, setUsers] = useState<UserListItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [booting, setBooting] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<UserListItem | null>(null);
  const [resettingUser, setResettingUser] = useState<UserListItem | null>(null);
  const [resetForm] = Form.useForm<{ newPassword: string }>();
  const [createForm] = Form.useForm<CreateFormValues>();
  const [editForm] = Form.useForm<EditFormValues>();
  const adminRole = roles.find((role) => role.code === 'ADMIN');

  const loadRoles = useCallback(async () => {
    const roleData = await apiRequest<Role[]>('/api/admin/roles');
    setRoles(roleData);
  }, []);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (keyword.trim()) {
        params.set('keyword', keyword.trim());
      }
      if (statusFilter) {
        params.set('status', statusFilter);
      }
      const queryString = params.toString();
      const path = `/api/admin/users${queryString ? `?${queryString}` : ''}`;
      const userData = await apiRequest<UserListItem[]>(path);
      setUsers(userData);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载账号失败');
    } finally {
      setLoading(false);
    }
  }, [keyword, message, statusFilter]);

  useEffect(() => {
    const session = readAuthSession();
    if (!session) {
      void router.replace('/login');
      return;
    }

    apiRequest<CurrentUser>('/api/auth/me')
      .then((user) => {
        setCurrentUser(user);
        return Promise.all([loadRoles(), loadUsers()]);
      })
      .catch((error) => {
        clearAuthSession();
        message.error(error instanceof Error ? error.message : '登录态已失效');
        void router.replace('/login');
      })
      .finally(() => setBooting(false));
  }, [loadRoles, loadUsers, message, router]);

  useEffect(() => {
    if (!booting && currentUser) {
      void loadUsers();
    }
  }, [booting, currentUser, loadUsers]);

  const handleCreate = async () => {
    const values = await createForm.validateFields();
    try {
      await apiRequest<UserListItem>('/api/admin/users', {
        method: 'POST',
        body: JSON.stringify(values)
      });
      message.success('账号已创建');
      setCreateOpen(false);
      createForm.resetFields();
      await loadUsers();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '创建账号失败');
    }
  };

  const openEdit = (user: UserListItem) => {
    setEditingUser(user);
    editForm.setFieldsValue({
      realName: user.realName,
      nickname: undefined,
      status: user.status,
      roleIds: user.roles.map((role) => role.id)
    });
  };

  const isAdminAccount = (user: UserListItem | null) => Boolean(user?.roles.some((role) => role.code === 'ADMIN'));

  const normalizeRoleSelection = (roleIds: number[]) => {
    if (!adminRole) {
      return roleIds;
    }
    return roleIds.includes(adminRole.id) ? [adminRole.id] : roleIds;
  };

  const handleEdit = async () => {
    if (!editingUser) {
      return;
    }
    const values = await editForm.validateFields();
    try {
      await apiRequest<UserListItem>(`/api/admin/users/${editingUser.id}`, {
        method: 'PUT',
        body: JSON.stringify(values)
      });
      message.success('账号已更新');
      setEditingUser(null);
      editForm.resetFields();
      await loadUsers();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '更新账号失败');
    }
  };

  const handleDelete = async (userId: number) => {
    try {
      await apiRequest<void>(`/api/admin/users/${userId}`, {
        method: 'DELETE'
      });
      message.success('账号已删除');
      await loadUsers();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '删除账号失败');
    }
  };

  const handleResetPassword = async () => {
    if (!resettingUser) {
      return;
    }
    const values = await resetForm.validateFields();
    try {
      await apiRequest<void>(`/api/admin/users/${resettingUser.id}/reset-password`, {
        method: 'POST',
        body: JSON.stringify({ newPassword: values.newPassword })
      });
      message.success('密码已重置');
      setResettingUser(null);
      resetForm.resetFields();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '重置密码失败');
    }
  };

  const handleLogout = async () => {
    try {
      await apiRequest<void>('/api/auth/logout', {
        method: 'POST'
      });
    } catch {
      // Ignore logout API failures and clear local session anyway.
    } finally {
      clearAuthSession();
      void router.replace('/login');
    }
  };

  const columns: ColumnsType<UserListItem> = [
    {
      title: '账号编号',
      dataIndex: 'userCode',
      key: 'userCode',
      width: 130
    },
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      width: 140
    },
    {
      title: '姓名',
      dataIndex: 'realName',
      key: 'realName',
      width: 140
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (value: UserListItem['status']) => (
        <Tag color={value === 'ACTIVE' ? 'green' : 'volcano'}>
          {value}
        </Tag>
      )
    },
    {
      title: '角色',
      key: 'roles',
      width: 220,
      render: (_, record) => (
        <Space wrap>
          {record.roles.map((role) => (
            <Tag key={role.id} color={role.code === 'ADMIN' ? 'gold' : 'blue'}>
              {role.name}
            </Tag>
          ))}
        </Space>
      )
    },
    {
      title: '操作',
      key: 'actions',
      width: 220,
      render: (_, record) => (
        <Space wrap>
          <Button size="small" onClick={() => openEdit(record)}>编辑</Button>
          <Button size="small" onClick={() => setResettingUser(record)}>重置密码</Button>
          <Popconfirm title="确认删除此账号？" onConfirm={() => handleDelete(record.id)} disabled={isAdminAccount(record)}>
            <Button size="small" danger disabled={isAdminAccount(record)}>删除</Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  if (booting) {
    return <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><Text>正在校验登录态...</Text></div>;
  }

  return (
    <EnterpriseShell
      currentUser={currentUser}
      onLogout={handleLogout}
      title="账号管理"
      subtitle="维护企业账号、角色与访问状态"
      section="admin-users"
    >
      <Card bordered={false} className="page-hero" style={{ marginBottom: 24 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} md={12}>
            <Title level={2} style={{ marginTop: 0 }}>账号权限管理</Title>
          </Col>
          <Col xs={24} md={12}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Text strong>当前登录账号</Text>
              <Text>{currentUser?.username} ({currentUser?.userCode})</Text>
              <Text type="secondary">账号状态：{currentUser?.status}</Text>
            </Space>
          </Col>
        </Row>
      </Card>

      <Card bordered={false} className="dashboard-card">
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Row gutter={[12, 12]}>
            <Col xs={24} md={10}>
              <Input.Search
                placeholder="搜索用户名、姓名、编号"
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                onSearch={() => void loadUsers()}
              />
            </Col>
            <Col xs={24} md={6}>
              <Select
                allowClear
                placeholder="状态筛选"
                style={{ width: '100%' }}
                value={statusFilter}
                onChange={(value) => setStatusFilter(value)}
                options={[
                  { label: 'ACTIVE', value: 'ACTIVE' },
                  { label: 'DISABLED', value: 'DISABLED' }
                ]}
              />
            </Col>
            <Col xs={24} md={8}>
              <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
                <Button onClick={() => void loadUsers()}>刷新</Button>
                <Button type="primary" style={{ background: '#183153' }} onClick={() => setCreateOpen(true)}>
                  新增账号
                </Button>
              </Space>
            </Col>
          </Row>

          <Table
            rowKey="id"
            columns={columns}
            dataSource={users}
            loading={loading}
            pagination={{ pageSize: 8 }}
            scroll={{ x: 1040 }}
          />
        </Space>
      </Card>

      <Modal
        title="新增账号"
        open={createOpen}
        onCancel={() => {
          setCreateOpen(false);
          createForm.resetFields();
        }}
        onOk={() => void handleCreate()}
        destroyOnClose
      >
        <Form layout="vertical" form={createForm} initialValues={{ status: 'ACTIVE' }}>
          <Form.Item label="用户名" name="username" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item label="真实姓名" name="realName" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item label="昵称" name="nickname">
            <Input />
          </Form.Item>
          <Form.Item label="初始密码" name="password" rules={[{ required: true, min: 8 }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item label="账号状态" name="status" rules={[{ required: true }]}>
            <Select options={[{ label: 'ACTIVE', value: 'ACTIVE' }, { label: 'DISABLED', value: 'DISABLED' }]} />
          </Form.Item>
          <Form.Item label="角色" name="roleIds" rules={[{ required: true }]}>
            <Select
              mode="multiple"
              onChange={(value) => createForm.setFieldValue('roleIds', normalizeRoleSelection(value))}
              options={roles.map((role) => ({ label: `${role.name} (${role.code})`, value: role.id }))}
            />
          </Form.Item>
          <Text type="secondary">管理员角色为固定全权限。选择 `ADMIN` 后会自动锁定为管理员账号。</Text>
        </Form>
      </Modal>

      <Modal
        title={editingUser ? `编辑账号 ${editingUser.username}` : '编辑账号'}
        open={Boolean(editingUser)}
        onCancel={() => {
          setEditingUser(null);
          editForm.resetFields();
        }}
        onOk={() => void handleEdit()}
        destroyOnClose
      >
        <Form layout="vertical" form={editForm}>
          <Form.Item label="真实姓名" name="realName" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item label="昵称" name="nickname">
            <Input />
          </Form.Item>
          <Form.Item label="账号状态" name="status" rules={[{ required: true }]}>
            <Select options={[{ label: 'ACTIVE', value: 'ACTIVE' }, { label: 'DISABLED', value: 'DISABLED' }]} />
          </Form.Item>
          {isAdminAccount(editingUser) ? (
            <Form.Item label="角色">
              <Space wrap>
                <Tag color="gold">系统管理员</Tag>
                <Text type="secondary">管理员权限固定为全开，不可修改。</Text>
              </Space>
            </Form.Item>
          ) : (
            <Form.Item label="角色" name="roleIds" rules={[{ required: true }]}>
              <Select
                mode="multiple"
                onChange={(value) => editForm.setFieldValue('roleIds', normalizeRoleSelection(value))}
                options={roles.map((role) => ({ label: `${role.name} (${role.code})`, value: role.id }))}
              />
            </Form.Item>
          )}
        </Form>
      </Modal>

      <Modal
        title={`重置密码 - ${resettingUser?.username}`}
        open={Boolean(resettingUser)}
        onCancel={() => {
          setResettingUser(null);
          resetForm.resetFields();
        }}
        onOk={() => void handleResetPassword()}
        destroyOnClose
      >
        <Form layout="vertical" form={resetForm}>
          <Form.Item label="新密码" name="newPassword" rules={[{ required: true, min: 8, message: '密码长度至少8位' }]}>
            <Input.Password placeholder="请输入新密码（至少8位）" />
          </Form.Item>
          <Form.Item label="确认密码" name="confirmPassword" dependencies={['newPassword']} rules={[
            { required: true, message: '请确认密码' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) {
                  return Promise.resolve();
                }
                return Promise.reject(new Error('两次输入的密码不一致'));
              }
            })
          ]}>
            <Input.Password placeholder="请再次输入新密码" />
          </Form.Item>
        </Form>
      </Modal>
    </EnterpriseShell>
  );
}
