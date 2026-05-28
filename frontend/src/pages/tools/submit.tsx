import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/router';
import { App, Button, Card, Empty, Form, Input, Select, Space, Tabs, Tag, Typography } from 'antd';
import { EnterpriseShell } from '../../components/enterprise-shell';
import { TagSelector } from '../../components/tag-selector';
import { apiRequest } from '../../lib/api';
import { clearAuthSession, readAuthSession, type CurrentUser } from '../../lib/auth';

const { Title, Paragraph, Text } = Typography;
const { TextArea } = Input;

type SubmitFormValues = {
  name: string;
  summary?: string;
  description: string;
  url?: string;
  usageGuide: string;
  tagIds: number[];
};

type WorkflowSubmitFormValues = {
  name: string;
  scenario: string;
  description: string;
  steps: string;
  toolIds: number[];
};

type ToolDetail = {
  id: number;
  name: string;
  summary?: string | null;
  description: string;
  url?: string | null;
  usageGuide: string;
  status: string;
  recommenderId: number;
  tags: { id: number; name: string }[];
};

type WorkflowDetail = {
  id: number;
};

type ToolOption = {
  id: number;
  name: string;
  summary?: string | null;
  description: string;
  status: string;
};

export default function ToolSubmitPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const requestedTab = typeof router.query.tab === 'string' ? router.query.tab : undefined;
  const editingToolId = typeof router.query.toolId === 'string' ? router.query.toolId : undefined;
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [submittingTool, setSubmittingTool] = useState(false);
  const [submittingWorkflow, setSubmittingWorkflow] = useState(false);
  const [booting, setBooting] = useState(true);
  const [tags, setTags] = useState<{ id: number; name: string }[]>([]);
  const [toolOptions, setToolOptions] = useState<ToolOption[]>([]);
  const [activeTab, setActiveTab] = useState<'tool' | 'workflow'>('tool');
  const [editingTool, setEditingTool] = useState<ToolDetail | null>(null);
  const [toolForm] = Form.useForm<SubmitFormValues>();
  const [workflowForm] = Form.useForm<WorkflowSubmitFormValues>();
  const isEditMode = useMemo(() => Boolean(editingToolId), [editingToolId]);

  useEffect(() => {
    if (!readAuthSession()) {
      void router.replace('/login');
      return;
    }

    apiRequest<CurrentUser>('/api/auth/me')
      .then((user) => {
        setCurrentUser(user);
        return Promise.all([
          apiRequest<{ id: number; name: string }[]>('/api/tags').then(setTags),
          apiRequest<ToolOption[]>('/api/tools?sortBy=createdAt&sortOrder=desc').then(setToolOptions),
          editingToolId
            ? apiRequest<ToolDetail>(`/api/tools/${editingToolId}`).then((detail) => {
              if (detail.recommenderId !== user.id || detail.status !== 'REJECTED') {
                throw new Error('只有自己提交且已驳回的工具才能在这里修改重提');
              }
              setEditingTool(detail);
              setActiveTab('tool');
              toolForm.setFieldsValue({
                name: detail.name,
                summary: detail.summary || '',
                description: detail.description,
                url: detail.url || '',
                usageGuide: detail.usageGuide,
                tagIds: detail.tags.map((tag) => tag.id)
              });
            })
            : Promise.resolve()
        ]);
      })
      .catch((error) => {
        clearAuthSession();
        message.error(error instanceof Error ? error.message : '登录态已失效');
        void router.replace('/login');
      })
      .finally(() => setBooting(false));
  }, [editingToolId, message, router, toolForm]);

  useEffect(() => {
    if (editingToolId) {
      setActiveTab('tool');
      return;
    }
    if (requestedTab === 'workflow' || requestedTab === 'tool') {
      setActiveTab(requestedTab);
    }
  }, [editingToolId, requestedTab]);

  const handleToolSubmit = async () => {
    const values = await toolForm.validateFields();
    setSubmittingTool(true);
    try {
      const detail = await apiRequest<ToolDetail>(editingToolId ? `/api/tools/${editingToolId}/resubmission` : '/api/tools/submissions', {
        method: editingToolId ? 'PUT' : 'POST',
        body: JSON.stringify(values)
      });
      message.success(editingToolId ? '工具已修改并重新提交' : '工具已提交');
      await router.push(`/tools/${detail.id}`);
    } catch (error) {
      message.error(error instanceof Error ? error.message : editingToolId ? '重新提交工具失败' : '提交工具失败');
    } finally {
      setSubmittingTool(false);
    }
  };

  const handleWorkflowSubmit = async () => {
    const values = await workflowForm.validateFields();
    setSubmittingWorkflow(true);
    try {
      const detail = await apiRequest<WorkflowDetail>('/api/workflows/submissions', {
        method: 'POST',
        body: JSON.stringify(values)
      });
      message.success('工作流已提交');
      await router.push(`/workflows/${detail.id}`);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '提交工作流失败');
    } finally {
      setSubmittingWorkflow(false);
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

  if (booting) {
    return <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><Text>正在准备提交页面...</Text></div>;
  }

  return (
    <EnterpriseShell
      currentUser={currentUser}
      onLogout={handleLogout}
      title="提交内容"
      subtitle={isEditMode ? '修改驳回工具并重新进入审核流程' : '工具和工作流都从这里进入平台，减少来回切换'}
      section="submit"
    >
      <Card bordered={false} className="page-hero" style={{ marginBottom: 24 }}>
        <Space direction="vertical" size={12}>
          <Tag color="blue">{isEditMode ? 'Resubmit Tool' : 'New Entry'}</Tag>
          <Title level={2} style={{ margin: 0 }}>{isEditMode ? '修改后重新提交工具' : '补充工具，或者沉淀一套工作流'}</Title>
          <Paragraph style={{ marginBottom: 0 }}>
            {isEditMode ? '驳回原因会保留在详情页，本次提交会生成新的审核记录。' : '工具适合单点能力沉淀，工作流适合把多个工具串成一套可复用的方法。'}
          </Paragraph>
        </Space>
      </Card>

      <Card bordered={false} className="dashboard-card">
        <Tabs
          activeKey={activeTab}
          onChange={(key) => setActiveTab(key as 'tool' | 'workflow')}
          items={[
            {
              key: 'tool',
              label: isEditMode ? '重新提交工具' : '提交工具',
              children: (
                <Form
                  form={toolForm}
                  layout="vertical"
                  initialValues={{
                    name: '',
                    summary: '',
                    description: '',
                    url: '',
                    usageGuide: '',
                    tagIds: []
                  }}
                >
                  <Form.Item label="工具名称" name="name" rules={[{ required: true, message: '请输入工具名称' }]}>
                    <Input size="large" placeholder="例如：Figma、Notion AI、Cursor" />
                  </Form.Item>
                  <Form.Item label="一句话摘要" name="summary">
                    <Input size="large" maxLength={512} placeholder="概括这个工具最核心的价值" />
                  </Form.Item>
                  <Form.Item label="工具描述" name="description" rules={[{ required: true, message: '请输入工具描述' }]}>
                    <TextArea rows={6} placeholder="适用场景、解决的问题、为什么值得推荐" />
                  </Form.Item>
                  <Form.Item label="标签" name="tagIds">
                    <TagSelector
                      options={tags}
                      value={toolForm.getFieldValue('tagIds') ?? []}
                      onChange={(value) => toolForm.setFieldValue('tagIds', value)}
                      onCreateTag={handleCreateTag}
                      placeholder="选择已有标签，或直接新建标签"
                    />
                  </Form.Item>
                  <Form.Item label="工具链接" name="url" rules={[{ required: true, message: '请输入工具链接' }, { type: 'url', message: '请输入合法的链接地址' }]}>
                    <Input size="large" placeholder="https://example.com" />
                  </Form.Item>
                  <Form.Item label="使用方法" name="usageGuide" rules={[{ required: true, message: '请输入使用方法' }]}>
                    <TextArea rows={8} placeholder="建议写清楚接入方式、常用步骤、团队最佳实践" />
                  </Form.Item>
                  <Space>
                    <Button type="primary" size="large" style={{ background: '#183153' }} loading={submittingTool} onClick={() => void handleToolSubmit()}>
                      {isEditMode ? '保存并重新提交' : '提交工具'}
                    </Button>
                    <Button size="large">
                      <Link href={editingTool ? `/tools/${editingTool.id}` : '/tools'}>{isEditMode ? '返回工具详情' : '返回工具列表'}</Link>
                    </Button>
                  </Space>
                </Form>
              )
            },
            {
              key: 'workflow',
              label: '提交工作流',
              disabled: isEditMode,
              children: toolOptions.length === 0 ? (
                <Empty
                  description="当前还没有可关联的已发布工具，建议先提交工具后再创建工作流。"
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                >
                  <Button type="primary" style={{ background: '#183153' }}>
                    <Link href="/tools">先去看看工具目录</Link>
                  </Button>
                </Empty>
              ) : (
                <Form
                  form={workflowForm}
                  layout="vertical"
                  initialValues={{
                    name: '',
                    scenario: '',
                    description: '',
                    steps: '',
                    toolIds: []
                  }}
                >
                  <Form.Item label="工作流名称" name="name" rules={[{ required: true, message: '请输入工作流名称' }]}>
                    <Input size="large" placeholder="例如：新品调研工作流、内容发布工作流" />
                  </Form.Item>
                  <Form.Item label="适用场景" name="scenario" rules={[{ required: true, message: '请输入适用场景' }]}>
                    <Input size="large" placeholder="例如：行业调研、需求梳理、内容策划" />
                  </Form.Item>
                  <Form.Item label="工作流描述" name="description" rules={[{ required: true, message: '请输入工作流描述' }]}>
                    <TextArea rows={5} placeholder="这个工作流解决什么问题，适合什么人使用" />
                  </Form.Item>
                  <Form.Item
                    label="关联工具"
                    name="toolIds"
                    rules={[{ required: true, message: '请至少选择一个工具' }]}
                    extra="按实际使用顺序选择，详情页会按这里的顺序展示。"
                  >
                    <Select
                      mode="multiple"
                      size="large"
                      placeholder="选择这个工作流会用到的工具"
                      optionFilterProp="label"
                      options={toolOptions.map((tool) => ({
                        value: tool.id,
                        label: tool.name,
                        title: tool.summary || tool.description
                      }))}
                    />
                  </Form.Item>
                  <Form.Item label="步骤说明" name="steps" rules={[{ required: true, message: '请输入步骤说明' }]}>
                    <TextArea rows={10} placeholder={'建议按顺序写清楚每一步，例如：\n1. 明确目标和输入材料\n2. 使用工具 A 做资料整理\n3. 使用工具 B 输出结果'} />
                  </Form.Item>
                  <Form.Item noStyle shouldUpdate={(prev, next) => prev.toolIds !== next.toolIds}>
                    {() => (
                      <Space direction="vertical" size={12} style={{ width: '100%', marginBottom: 16 }}>
                        <Text strong>已选工具预览</Text>
                        <Space wrap>
                          {((workflowForm.getFieldValue('toolIds') ?? []) as number[]).map((toolId: number) => {
                            const tool = toolOptions.find((item) => item.id === toolId);
                            return tool ? <Tag key={tool.id}>{tool.name}</Tag> : null;
                          })}
                        </Space>
                      </Space>
                    )}
                  </Form.Item>
                  <Space>
                    <Button type="primary" size="large" style={{ background: '#183153' }} loading={submittingWorkflow} onClick={() => void handleWorkflowSubmit()}>
                      提交工作流
                    </Button>
                    <Button size="large">
                      <Link href="/workflows">返回工作流目录</Link>
                    </Button>
                  </Space>
                </Form>
              )
            }
          ]}
        />
      </Card>
    </EnterpriseShell>
  );
}
