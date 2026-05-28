import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import { App, Button, Card, Form, Input, Modal, Popconfirm, Space, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { EnterpriseShell } from '../../components/enterprise-shell';
import { apiRequest } from '../../lib/api';
import { clearAuthSession, readAuthSession, type CurrentUser } from '../../lib/auth';

const { Title, Text } = Typography;

type TagItem = {
  id: number;
  name: string;
  description?: string | null;
  createdAt: string;
  updatedAt: string;
};

type TagFormValues = {
  name: string;
  description?: string;
};

export default function AdminTagsPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [tags, setTags] = useState<TagItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [booting, setBooting] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [editingTag, setEditingTag] = useState<TagItem | null>(null);
  const [createForm] = Form.useForm<TagFormValues>();
  const [editForm] = Form.useForm<TagFormValues>();

  const loadTags = useCallback(async () => {
    setLoading(true);
    try {
      const data = await apiRequest<TagItem[]>('/api/admin/tags');
      setTags(data);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载标签失败');
    } finally {
      setLoading(false);
    }
  }, [message]);

  useEffect(() => {
    if (!readAuthSession()) {
      void router.replace('/login');
      return;
    }

    apiRequest<CurrentUser>('/api/auth/me')
      .then((user) => {
        if (!user.roleCodes.includes('ADMIN')) {
          message.error('当前账号无权访问标签管理页');
          void router.replace('/tools');
          return;
        }
        setCurrentUser(user);
        return loadTags();
      })
      .catch((error) => {
        clearAuthSession();
        message.error(error instanceof Error ? error.message : '登录态已失效');
        void router.replace('/login');
      })
      .finally(() => setBooting(false));
  }, [loadTags, message, router]);

  const handleCreate = async () => {
    const values = await createForm.validateFields();
    try {
      await apiRequest<TagItem>('/api/admin/tags', {
        method: 'POST',
        body: JSON.stringify(values)
      });
      message.success('标签已创建');
      setCreateOpen(false);
      createForm.resetFields();
      await loadTags();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '创建标签失败');
    }
  };

  const handleEdit = async () => {
    if (!editingTag) {
      return;
    }
    const values = await editForm.validateFields();
    try {
      await apiRequest<TagItem>(`/api/admin/tags/${editingTag.id}`, {
        method: 'PUT',
        body: JSON.stringify(values)
      });
      message.success('标签已更新');
      setEditingTag(null);
      editForm.resetFields();
      await loadTags();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '更新标签失败');
    }
  };

  const handleDelete = async (tagId: number) => {
    try {
      await apiRequest<void>(`/api/admin/tags/${tagId}`, {
        method: 'DELETE'
      });
      message.success('标签已删除');
      await loadTags();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '删除标签失败');
    }
  };

  const handleLogout = async () => {
    try {
      await apiRequest<void>('/api/auth/logout', { method: 'POST' });
    } catch {
      // Ignore logout failures and clear local session anyway.
    } finally {
      clearAuthSession();
      void router.replace('/login');
    }
  };

  const columns: ColumnsType<TagItem> = [
    { title: '标签名称', dataIndex: 'name', key: 'name', width: 180 },
    { title: '描述', dataIndex: 'description', key: 'description', render: (value?: string | null) => value || '暂无描述' },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 220,
      render: (value: string) => new Date(value).toLocaleString('zh-CN')
    },
    {
      title: '操作',
      key: 'actions',
      width: 180,
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => {
            setEditingTag(record);
            editForm.setFieldsValue({ name: record.name, description: record.description || '' });
          }}>
            编辑
          </Button>
          <Popconfirm title="确认删除此标签？" onConfirm={() => void handleDelete(record.id)}>
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  if (booting) {
    return <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><Text>正在加载标签管理...</Text></div>;
  }

  return (
    <EnterpriseShell
      currentUser={currentUser}
      onLogout={handleLogout}
      title="标签管理"
      subtitle="维护标签池，供工具与工作流复用"
      section="admin-tags"
    >
      <Card bordered={false} className="page-hero" style={{ marginBottom: 24 }}>
        <Space direction="vertical" size={12}>
          <Title level={2} style={{ margin: 0 }}>统一维护工具标签</Title>
        </Space>
      </Card>

      <Card
        bordered={false}
        className="dashboard-card"
        title="标签列表"
        extra={<Space><Button onClick={() => void loadTags()}>刷新</Button><Button type="primary" style={{ background: '#183153' }} onClick={() => setCreateOpen(true)}>新增标签</Button></Space>}
      >
        <Table rowKey="id" columns={columns} dataSource={tags} loading={loading} pagination={{ pageSize: 8 }} />
      </Card>

      <Modal title="新增标签" open={createOpen} onCancel={() => { setCreateOpen(false); createForm.resetFields(); }} onOk={() => void handleCreate()}>
        <Form form={createForm} layout="vertical">
          <Form.Item label="标签名称" name="name" rules={[{ required: true, message: '请输入标签名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="标签描述" name="description">
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={editingTag ? `编辑标签 ${editingTag.name}` : '编辑标签'} open={Boolean(editingTag)} onCancel={() => { setEditingTag(null); editForm.resetFields(); }} onOk={() => void handleEdit()}>
        <Form form={editForm} layout="vertical">
          <Form.Item label="标签名称" name="name" rules={[{ required: true, message: '请输入标签名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="标签描述" name="description">
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </EnterpriseShell>
  );
}
