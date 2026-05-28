import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/router';
import { App, Button, Card, Col, Form, Input, Modal, Popconfirm, Row, Select, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { EnterpriseShell } from '../../components/enterprise-shell';
import { TagSelector } from '../../components/tag-selector';
import { apiRequest } from '../../lib/api';
import { clearAuthSession, readAuthSession, type CurrentUser } from '../../lib/auth';

const { Title, Text } = Typography;
const { TextArea } = Input;

type ToolListItem = {
  id: number;
  name: string;
  summary?: string | null;
  description: string;
  url?: string | null;
  status: 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'OFFLINE';
  recommenderId: number;
  recommenderName: string;
  starCount: number;
  favoriteCount: number;
  commentCount: number;
  tags: { id: number; name: string }[];
  createdAt: string;
  updatedAt: string;
};

type ToolDetail = ToolListItem & {
  usageGuide: string;
  tagIds: number[];
};

type ToolFormValues = {
  name: string;
  summary?: string;
  description: string;
  url?: string;
  usageGuide: string;
  tagIds: number[];
};

function statusColor(status: ToolListItem['status']) {
  if (status === 'APPROVED') {
    return 'green';
  }
  if (status === 'PENDING_REVIEW') {
    return 'gold';
  }
  if (status === 'REJECTED') {
    return 'red';
  }
  if (status === 'OFFLINE') {
    return 'volcano';
  }
  return 'blue';
}

export default function AdminToolsPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState<string | undefined>();
  const [loading, setLoading] = useState(false);
  const [booting, setBooting] = useState(true);
  const [tools, setTools] = useState<ToolListItem[]>([]);
  const [tags, setTags] = useState<{ id: number; name: string }[]>([]);
  const [selectedTagIds, setSelectedTagIds] = useState<number[]>([]);
  const [sortBy, setSortBy] = useState<'createdAt' | 'starCount'>('createdAt');
  const [sortOrder, setSortOrder] = useState<'desc' | 'asc'>('desc');
  const [createOpen, setCreateOpen] = useState(false);
  const [editingTool, setEditingTool] = useState<ToolDetail | null>(null);
  const [createForm] = Form.useForm<ToolFormValues>();
  const [editForm] = Form.useForm<ToolFormValues>();

  const loadTools = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (keyword.trim()) {
        params.set('keyword', keyword.trim());
      }
      if (statusFilter) {
        params.set('status', statusFilter);
      }
      selectedTagIds.forEach((tagId) => params.append('tagIds', String(tagId)));
      params.set('sortBy', sortBy);
      params.set('sortOrder', sortOrder);
      const query = params.toString();
      const data = await apiRequest<ToolListItem[]>(`/api/admin/tools${query ? `?${query}` : ''}`);
      setTools(data);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载工具失败');
    } finally {
      setLoading(false);
    }
  }, [keyword, message, selectedTagIds, sortBy, sortOrder, statusFilter]);

  useEffect(() => {
    if (!readAuthSession()) {
      void router.replace('/login');
      return;
    }

    apiRequest<CurrentUser>('/api/auth/me')
      .then((user) => {
        if (!user.roleCodes.includes('ADMIN')) {
          message.error('当前账号无权访问工具管理页');
          void router.replace('/tools');
          return;
        }
        setCurrentUser(user);
        return Promise.all([
          loadTools(),
          apiRequest<{ id: number; name: string }[]>('/api/admin/tags').then(setTags)
        ]);
      })
      .catch((error) => {
        clearAuthSession();
        message.error(error instanceof Error ? error.message : '登录态已失效');
        void router.replace('/login');
      })
      .finally(() => setBooting(false));
  }, [loadTools, message, router]);

  const handleCreate = async () => {
    const values = await createForm.validateFields();
    try {
      await apiRequest<ToolDetail>('/api/admin/tools', {
        method: 'POST',
        body: JSON.stringify(values)
      });
      message.success('工具已创建');
      setCreateOpen(false);
      createForm.resetFields();
      await loadTools();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '创建工具失败');
    }
  };

  const openEdit = async (toolId: number) => {
    try {
      const detail = await apiRequest<ToolDetail>(`/api/admin/tools/${toolId}`);
      setEditingTool(detail);
      editForm.setFieldsValue({
        name: detail.name,
        summary: detail.summary || '',
        description: detail.description,
        url: detail.url || '',
        usageGuide: detail.usageGuide,
        tagIds: detail.tags.map((tag) => tag.id)
      });
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载工具详情失败');
    }
  };

  const handleEdit = async () => {
    if (!editingTool) {
      return;
    }
    const values = await editForm.validateFields();
    try {
      await apiRequest<ToolDetail>(`/api/admin/tools/${editingTool.id}`, {
        method: 'PUT',
        body: JSON.stringify(values)
      });
      message.success('工具已更新');
      setEditingTool(null);
      editForm.resetFields();
      await loadTools();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '更新工具失败');
    }
  };

  const handleDelete = async (toolId: number) => {
    try {
      await apiRequest<void>(`/api/admin/tools/${toolId}`, {
        method: 'DELETE'
      });
      message.success('工具已删除');
      await loadTools();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '删除工具失败');
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

  const handleCreateTag = async (name: string) => {
    const created = await apiRequest<{ id: number; name: string }>('/api/tags', {
      method: 'POST',
      body: JSON.stringify({ name, description: '' })
    });
    setTags((current) => {
      if (current.some((tag) => tag.id === created.id)) {
        return current;
      }
      return [...current, created].sort((left, right) => left.name.localeCompare(right.name, 'zh-CN'));
    });
    message.success(`标签“${created.name}”已创建`);
    return created;
  };

  const columns: ColumnsType<ToolListItem> = [
    {
      title: '工具名称',
      dataIndex: 'name',
      key: 'name',
      width: 160,
      render: (_, record) => <Link href={`/tools/${record.id}`} style={{ fontWeight: 600, fontSize: 15 }}>{record.name}</Link>
    },
    {
      title: '摘要',
      dataIndex: 'summary',
      key: 'summary',
      render: (value: string | null | undefined, record) => (
        <div>
          <div style={{ color: 'rgba(0,0,0,0.85)', marginBottom: 6, lineHeight: 1.6 }}>
            {value || '暂无摘要'}
          </div>
          <Space size={8} style={{ marginTop: 4 }}>
            {record.tags.map((tag) => (
              <Tag key={tag.id} style={{ fontSize: 11, padding: '0 6px', margin: 0 }}>{tag.name}</Tag>
            ))}
            <Tag color={statusColor(record.status)} style={{ fontSize: 11, padding: '0 6px', margin: 0 }}>{record.status}</Tag>
          </Space>
        </div>
      )
    },
    {
      title: '推荐人',
      dataIndex: 'recommenderName',
      key: 'recommenderName',
      width: 100,
      ellipsis: true
    },
    {
      title: '链接',
      dataIndex: 'url',
      key: 'url',
      width: 200,
      ellipsis: true,
      render: (value?: string | null) => value ? (
        <a href={value} target="_blank" rel="noreferrer" style={{ fontSize: 12 }}>
          {value}
        </a>
      ) : <span style={{ fontSize: 12, color: 'rgba(0,0,0,0.45)' }}>未提供</span>
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 160,
      render: (value: string) => <span style={{ fontSize: 12, color: 'rgba(0,0,0,0.45)' }}>{new Date(value).toLocaleString('zh-CN')}</span>
    },
    {
      title: '操作',
      key: 'actions',
      width: 180,
      render: (_, record) => (
        <Space wrap>
          <Button size="small" onClick={() => void openEdit(record.id)}>编辑</Button>
          <Popconfirm title="确认删除此工具？" onConfirm={() => void handleDelete(record.id)}>
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  if (booting) {
    return <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><Text>正在校验管理权限...</Text></div>;
  }

  return (
    <EnterpriseShell
      currentUser={currentUser}
      onLogout={handleLogout}
      title="工具管理"
      subtitle="统一处理工具目录与工作流配置"
      section="admin-tools"
    >
          <Card bordered={false} className="page-hero" style={{ marginBottom: 24 }}>
            <Row gutter={[16, 16]} align="middle">
              <Col xs={24} md={14}>
                <Title level={2} style={{ marginTop: 0 }}>工具基础管理</Title>
              </Col>
              <Col xs={24} md={10}>
                <Space direction="vertical" style={{ width: '100%' }}>
                  <Text strong>当前工具总数</Text>
                  <Title level={3} style={{ margin: 0 }}>{tools.length}</Title>
                  <Space wrap>
                    <Button style={{ width: 'fit-content' }}>
                      <Link href="/tools/submit?tab=workflow">提交工作流</Link>
                    </Button>
                    <Button style={{ width: 'fit-content' }}>
                      <Link href="/admin/workflows">管理工作流</Link>
                    </Button>
                  </Space>
                </Space>
              </Col>
            </Row>
          </Card>

          <Card bordered={false} className="dashboard-card">
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <Row gutter={[12, 12]}>
                <Col xs={24} md={10}>
                  <Input.Search
                    placeholder="搜索工具名称、摘要或描述"
                    value={keyword}
                    onChange={(event) => setKeyword(event.target.value)}
                    onSearch={() => void loadTools()}
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
                      { label: 'DRAFT', value: 'DRAFT' },
                      { label: 'PENDING_REVIEW', value: 'PENDING_REVIEW' },
                      { label: 'APPROVED', value: 'APPROVED' },
                      { label: 'REJECTED', value: 'REJECTED' }
                    ]}
                  />
                </Col>
                <Col xs={24} md={8}>
                  <Select
                    mode="multiple"
                    allowClear
                    placeholder="标签筛选"
                    style={{ width: '100%' }}
                    value={selectedTagIds}
                    onChange={(values) => setSelectedTagIds(values)}
                    options={tags.map((tag) => ({ label: tag.name, value: tag.id }))}
                  />
                </Col>
                <Col xs={24} md={5}>
                  <Select
                    value={sortBy}
                    onChange={(value) => setSortBy(value)}
                    options={[
                      { label: '按时间排序', value: 'createdAt' },
                      { label: '按 Star 排序', value: 'starCount' }
                    ]}
                  />
                </Col>
                <Col xs={24} md={5}>
                  <Select
                    value={sortOrder}
                    onChange={(value) => setSortOrder(value)}
                    options={[
                      { label: '降序', value: 'desc' },
                      { label: '升序', value: 'asc' }
                    ]}
                  />
                </Col>
                <Col xs={24} md={24}>
                  <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
                    <Button onClick={() => void loadTools()}>刷新</Button>
                    <Button>
                      <Link href="/tools/submit?tab=workflow">提交工作流</Link>
                    </Button>
                    <Button type="primary" style={{ background: '#183153' }} onClick={() => setCreateOpen(true)}>
                      新增工具
                    </Button>
                  </Space>
                </Col>
              </Row>

              <Table
                rowKey="id"
                columns={columns}
                dataSource={tools}
                loading={loading}
                pagination={{ pageSize: 8 }}
                scroll={{ x: 1120 }}
              />
            </Space>
          </Card>

      <Modal
        title="新增工具"
        open={createOpen}
        onCancel={() => {
          setCreateOpen(false);
          createForm.resetFields();
        }}
        onOk={() => void handleCreate()}
        destroyOnClose
        width={720}
      >
        <Form form={createForm} layout="vertical" initialValues={{ url: '', tagIds: [] }}>
          <Form.Item label="工具名称" name="name" rules={[{ required: true, message: '请输入工具名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="一句话摘要" name="summary">
            <Input />
          </Form.Item>
          <Form.Item label="工具描述" name="description" rules={[{ required: true, message: '请输入工具描述' }]}>
            <TextArea rows={4} />
          </Form.Item>
          <Form.Item label="标签" name="tagIds">
            <TagSelector
              options={tags}
              value={createForm.getFieldValue('tagIds') ?? []}
              onChange={(value) => createForm.setFieldValue('tagIds', value)}
              onCreateTag={handleCreateTag}
              placeholder="选择已有标签，或直接新建"
            />
          </Form.Item>
          <Form.Item label="工具链接" name="url" rules={[{ required: true, message: '请输入工具链接' }, { type: 'url', message: '请输入合法链接' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="使用方法" name="usageGuide" rules={[{ required: true, message: '请输入使用方法' }]}>
            <TextArea rows={5} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingTool ? `编辑工具 ${editingTool.name}` : '编辑工具'}
        open={Boolean(editingTool)}
        onCancel={() => {
          setEditingTool(null);
          editForm.resetFields();
        }}
        onOk={() => void handleEdit()}
        destroyOnClose
        width={720}
      >
        <Form form={editForm} layout="vertical">
          <Form.Item label="工具名称" name="name" rules={[{ required: true, message: '请输入工具名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="一句话摘要" name="summary">
            <Input />
          </Form.Item>
          <Form.Item label="工具描述" name="description" rules={[{ required: true, message: '请输入工具描述' }]}>
            <TextArea rows={4} />
          </Form.Item>
          <Form.Item label="标签" name="tagIds">
            <TagSelector
              options={tags}
              value={editForm.getFieldValue('tagIds') ?? []}
              onChange={(value) => editForm.setFieldValue('tagIds', value)}
              onCreateTag={handleCreateTag}
              placeholder="选择已有标签，或直接新建"
            />
          </Form.Item>
          <Form.Item label="工具链接" name="url" rules={[{ type: 'url', message: '请输入合法链接' }]} extra="链接不是必填项。">
            <Input />
          </Form.Item>
          <Form.Item label="使用方法" name="usageGuide" rules={[{ required: true, message: '请输入使用方法' }]}>
            <TextArea rows={5} />
          </Form.Item>
        </Form>
      </Modal>
    </EnterpriseShell>
  );
}
