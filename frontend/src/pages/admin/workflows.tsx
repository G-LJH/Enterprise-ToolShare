import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
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
  Switch,
  Table,
  Tag,
  Typography
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { EnterpriseShell } from '../../components/enterprise-shell';
import { apiRequest } from '../../lib/api';
import { clearAuthSession, readAuthSession, type CurrentUser } from '../../lib/auth';

const { Title, Text } = Typography;
const { TextArea } = Input;

type WorkflowListItem = {
  id: number;
  name: string;
  scenario: string;
  description: string;
  featured: boolean;
  creatorId: number;
  creatorName: string;
  toolCount: number;
  tools: { id: number; name: string; summary?: string | null; url: string }[];
  createdAt: string;
  updatedAt: string;
};

type WorkflowDetail = {
  id: number;
  name: string;
  scenario: string;
  description: string;
  steps: string;
  featured: boolean;
  creatorId: number;
  creatorName: string;
  tools: { id: number; name: string; summary?: string | null; description: string; url: string; tags: { id: number; name: string }[] }[];
  createdAt: string;
  updatedAt: string;
};

type ToolOption = {
  id: number;
  name: string;
  summary?: string | null;
  description: string;
  url: string;
  status: string;
  recommenderId: number;
  recommenderName: string;
  starCount: number;
  favoriteCount: number;
  commentCount: number;
  tags: { id: number; name: string }[];
  createdAt: string;
  updatedAt: string;
};

type WorkflowFormValues = {
  name: string;
  scenario: string;
  description: string;
  steps: string;
  featured: boolean;
  toolIds: number[];
};

export default function AdminWorkflowsPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [scenarioFilter, setScenarioFilter] = useState('');
  const [workflows, setWorkflows] = useState<WorkflowListItem[]>([]);
  const [tools, setTools] = useState<ToolOption[]>([]);
  const [loading, setLoading] = useState(false);
  const [booting, setBooting] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [editingWorkflow, setEditingWorkflow] = useState<WorkflowDetail | null>(null);
  const [createForm] = Form.useForm<WorkflowFormValues>();
  const [editForm] = Form.useForm<WorkflowFormValues>();

  const loadWorkflows = useCallback(async () => {
    setLoading(true);
    try {
      const query = scenarioFilter.trim() ? `?scenario=${encodeURIComponent(scenarioFilter.trim())}` : '';
      const data = await apiRequest<WorkflowListItem[]>(`/api/admin/workflows${query}`);
      setWorkflows(data);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载工作流失败');
    } finally {
      setLoading(false);
    }
  }, [message, scenarioFilter]);

  useEffect(() => {
    if (!readAuthSession()) {
      void router.replace('/login');
      return;
    }

    apiRequest<CurrentUser>('/api/auth/me')
      .then((user) => {
        if (!user.roleCodes.includes('ADMIN')) {
          message.error('当前账号无权访问工作流管理页');
          void router.replace('/tools');
          return;
        }
        setCurrentUser(user);
        return Promise.all([
          loadWorkflows(),
          apiRequest<ToolOption[]>('/api/admin/tools').then(setTools)
        ]);
      })
      .catch((error) => {
        clearAuthSession();
        message.error(error instanceof Error ? error.message : '登录态已失效');
        void router.replace('/login');
      })
      .finally(() => setBooting(false));
  }, [loadWorkflows, message, router]);

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

  const handleCreate = async () => {
    const values = await createForm.validateFields();
    try {
      await apiRequest('/api/admin/workflows', {
        method: 'POST',
        body: JSON.stringify(values)
      });
      message.success('工作流已创建');
      setCreateOpen(false);
      createForm.resetFields();
      await loadWorkflows();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '创建工作流失败');
    }
  };

  const openEdit = async (workflowId: number) => {
    try {
      const detail = await apiRequest<WorkflowDetail>(`/api/admin/workflows/${workflowId}`);
      setEditingWorkflow(detail);
      editForm.setFieldsValue({
        name: detail.name,
        scenario: detail.scenario,
        description: detail.description,
        steps: detail.steps,
        featured: detail.featured,
        toolIds: detail.tools.map((tool) => tool.id)
      });
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载工作流详情失败');
    }
  };

  const handleEdit = async () => {
    if (!editingWorkflow) {
      return;
    }
    const values = await editForm.validateFields();
    try {
      await apiRequest(`/api/admin/workflows/${editingWorkflow.id}`, {
        method: 'PUT',
        body: JSON.stringify(values)
      });
      message.success('工作流已更新');
      setEditingWorkflow(null);
      editForm.resetFields();
      await loadWorkflows();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '更新工作流失败');
    }
  };

  const handleDelete = async (workflowId: number) => {
    try {
      await apiRequest(`/api/admin/workflows/${workflowId}`, {
        method: 'DELETE'
      });
      message.success('工作流已删除');
      await loadWorkflows();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '删除工作流失败');
    }
  };

  const columns: ColumnsType<WorkflowListItem> = [
    {
      title: '工作流名称',
      dataIndex: 'name',
      key: 'name',
      render: (_, record) => <Link href={`/workflows/${record.id}`}>{record.name}</Link>
    },
    {
      title: '场景',
      dataIndex: 'scenario',
      key: 'scenario',
      width: 160,
      render: (value: string, record) => (
        <Space wrap>
          <Tag>{value}</Tag>
          {record.featured ? <Tag color="gold">精选</Tag> : null}
        </Space>
      )
    },
    {
      title: '创建人',
      dataIndex: 'creatorName',
      key: 'creatorName',
      width: 140
    },
    {
      title: '关联工具',
      key: 'tools',
      width: 280,
      render: (_, record) => (
        <Space wrap>
          {record.tools.map((tool) => (
            <Tag key={tool.id}>{tool.name}</Tag>
          ))}
        </Space>
      )
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 180,
      render: (value: string) => new Date(value).toLocaleString('zh-CN')
    },
    {
      title: '操作',
      key: 'actions',
      width: 180,
      render: (_, record) => (
        <Space wrap>
          <Button size="small" onClick={() => void openEdit(record.id)}>编辑</Button>
          <Popconfirm title="确认删除这个工作流？" onConfirm={() => void handleDelete(record.id)}>
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  const renderWorkflowForm = (form: typeof createForm) => (
    <Form form={form} layout="vertical" initialValues={{ featured: false, toolIds: [] }}>
      <Form.Item label="工作流名称" name="name" rules={[{ required: true, message: '请输入工作流名称' }]}>
        <Input />
      </Form.Item>
      <Form.Item label="适用场景" name="scenario" rules={[{ required: true, message: '请输入适用场景' }]}>
        <Input placeholder="例如：行业调研、PPT 制作、需求梳理" />
      </Form.Item>
      <Form.Item label="工作流描述" name="description" rules={[{ required: true, message: '请输入工作流描述' }]}>
        <TextArea rows={4} />
      </Form.Item>
      <Form.Item label="步骤说明" name="steps" rules={[{ required: true, message: '请输入步骤说明' }]}>
        <TextArea rows={6} />
      </Form.Item>
      <Form.Item label="关联工具" name="toolIds" rules={[{ required: true, message: '请至少选择一个工具' }]}>
        <Select
          mode="multiple"
          options={tools.map((tool) => ({
            label: `${tool.name} · ${tool.summary || tool.recommenderName}`,
            value: tool.id
          }))}
        />
      </Form.Item>
      <Form.Item label="设为精选" name="featured" valuePropName="checked">
        <Switch checkedChildren="精选" unCheckedChildren="普通" />
      </Form.Item>
    </Form>
  );

  if (booting) {
    return <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><Text>正在加载工作流管理页...</Text></div>;
  }

  return (
    <EnterpriseShell
      currentUser={currentUser}
      onLogout={handleLogout}
      title="工具管理"
      subtitle="在统一管理入口下维护工作流"
      section="admin-tools"
    >
      <Card bordered={false} className="page-hero" style={{ marginBottom: 24 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} md={16}>
            <Title level={2} style={{ marginTop: 0 }}>沉淀可复用的工具组合方案</Title>
          </Col>
          <Col xs={24} md={8}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Text strong>当前工作流数量</Text>
              <Title level={3} style={{ margin: 0 }}>{workflows.length}</Title>
            </Space>
          </Col>
        </Row>
      </Card>

      <Card bordered={false} className="dashboard-card">
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Row gutter={[12, 12]}>
            <Col xs={24} md={10}>
              <Input.Search
                placeholder="按场景筛选工作流"
                value={scenarioFilter}
                onChange={(event) => setScenarioFilter(event.target.value)}
                onSearch={() => void loadWorkflows()}
              />
            </Col>
            <Col xs={24} md={14}>
              <Space style={{ width: '100%', justifyContent: 'flex-end' }} wrap>
                <Button onClick={() => void loadWorkflows()} loading={loading}>刷新</Button>
                <Button type="primary" style={{ background: '#183153' }} onClick={() => setCreateOpen(true)}>
                  新增工作流
                </Button>
              </Space>
            </Col>
          </Row>
          <Table rowKey="id" columns={columns} dataSource={workflows} loading={loading} pagination={{ pageSize: 8 }} scroll={{ x: 1120 }} />
        </Space>
      </Card>

      <Modal title="新增工作流" open={createOpen} onCancel={() => { setCreateOpen(false); createForm.resetFields(); }} onOk={() => void handleCreate()} width={760}>
        {renderWorkflowForm(createForm)}
      </Modal>

      <Modal title={editingWorkflow ? `编辑 ${editingWorkflow.name}` : '编辑工作流'} open={Boolean(editingWorkflow)} onCancel={() => { setEditingWorkflow(null); editForm.resetFields(); }} onOk={() => void handleEdit()} width={760}>
        {renderWorkflowForm(editForm)}
      </Modal>
    </EnterpriseShell>
  );
}
