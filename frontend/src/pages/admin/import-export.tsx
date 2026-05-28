import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import { App, Button, Card, Checkbox, Col, Row, Space, Table, Tag, Typography, Upload } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { EnterpriseShell } from '../../components/enterprise-shell';
import { apiRequest } from '../../lib/api';
import { clearAuthSession, readAuthSession, type CurrentUser } from '../../lib/auth';

const { Title, Text } = Typography;

type ImportPreview = {
  totalRows: number;
  validRows: number;
  invalidRows: number;
  errors: string[];
};

type ImportTask = {
  id: number;
  taskName: string;
  fileName: string;
  status: string;
  successCount: number;
  failureCount: number;
  errorReportPath?: string | null;
  detail?: string | null;
  createdAt: string;
  updatedAt: string;
};

type ExportTask = {
  id: number;
  taskName: string;
  fileName: string;
  status: string;
  successCount: number;
  failureCount: number;
  errorReportPath?: string | null;
  detail?: string | null;
  content?: string | null;
  createdAt: string;
  updatedAt: string;
};

const exportFieldOptions = [
  'name', 'summary', 'description', 'url', 'usageGuide',
  'recommenderName', 'status', 'starCount', 'favoriteCount', 'commentCount', 'tags', 'updatedAt'
];

export default function AdminImportExportPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [booting, setBooting] = useState(true);
  const [loading, setLoading] = useState(false);
  const [csvFileName, setCsvFileName] = useState('');
  const [csvContent, setCsvContent] = useState('');
  const [preview, setPreview] = useState<ImportPreview | null>(null);
  const [importTasks, setImportTasks] = useState<ImportTask[]>([]);
  const [exportTasks, setExportTasks] = useState<ExportTask[]>([]);
  const [selectedFields, setSelectedFields] = useState<string[]>(['name', 'url', 'tags']);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [imports, exports] = await Promise.all([
        apiRequest<ImportTask[]>('/api/admin/import-export/imports'),
        apiRequest<ExportTask[]>('/api/admin/import-export/exports')
      ]);
      setImportTasks(imports);
      setExportTasks(exports);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载任务列表失败');
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
          message.error('当前账号无权访问导入导出管理页');
          void router.replace('/tools');
          return;
        }
        setCurrentUser(user);
        return loadData();
      })
      .catch((error) => {
        clearAuthSession();
        message.error(error instanceof Error ? error.message : '登录态已失效');
        void router.replace('/login');
      })
      .finally(() => setBooting(false));
  }, [loadData, message, router]);

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

  const downloadContent = (fileName: string, content: string) => {
    const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.click();
    URL.revokeObjectURL(url);
  };

  const handleDownloadTemplate = async () => {
    try {
      const file = await apiRequest<{ fileName: string; content: string }>('/api/admin/import-export/template');
      downloadContent(file.fileName, file.content);
      message.success('模板已下载');
    } catch (error) {
      message.error(error instanceof Error ? error.message : '下载模板失败');
    }
  };

  const handlePreview = async () => {
    try {
      const result = await apiRequest<ImportPreview>('/api/admin/import-export/imports/preview', {
        method: 'POST',
        body: JSON.stringify({ fileName: csvFileName || 'tools.csv', csvContent })
      });
      setPreview(result);
      message.success('预校验完成');
    } catch (error) {
      message.error(error instanceof Error ? error.message : '预校验失败');
    }
  };

  const handleImport = async () => {
    try {
      await apiRequest<ImportTask>('/api/admin/import-export/imports', {
        method: 'POST',
        body: JSON.stringify({ fileName: csvFileName || 'tools.csv', csvContent })
      });
      message.success('导入任务已执行');
      await loadData();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '导入失败');
    }
  };

  const handleExport = async () => {
    try {
      const task = await apiRequest<ExportTask>('/api/admin/import-export/exports', {
        method: 'POST',
        body: JSON.stringify({ fields: selectedFields })
      });
      if (task.content) {
        downloadContent(task.fileName, task.content);
      }
      message.success('导出文件已生成');
      await loadData();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '导出失败');
    }
  };

  const importColumns: ColumnsType<ImportTask> = [
    { title: '任务', dataIndex: 'taskName', key: 'taskName' },
    { title: '文件名', dataIndex: 'fileName', key: 'fileName' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (value: string) => <Tag>{value}</Tag> },
    { title: '成功', dataIndex: 'successCount', key: 'successCount', width: 90 },
    { title: '失败', dataIndex: 'failureCount', key: 'failureCount', width: 90 },
    { title: '详情', dataIndex: 'detail', key: 'detail', render: (value?: string | null) => value || '暂无' }
  ];

  const exportColumns: ColumnsType<ExportTask> = [
    { title: '任务', dataIndex: 'taskName', key: 'taskName' },
    { title: '文件名', dataIndex: 'fileName', key: 'fileName' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (value: string) => <Tag>{value}</Tag> },
    { title: '导出行数', dataIndex: 'successCount', key: 'successCount', width: 100 },
    { title: '详情', dataIndex: 'detail', key: 'detail', render: (value?: string | null) => value || '暂无' }
  ];

  if (booting) {
    return <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><Text>正在加载导入导出管理页...</Text></div>;
  }

  return (
    <EnterpriseShell
      currentUser={currentUser}
      onLogout={handleLogout}
      title="导入导出"
      subtitle="通过批量任务维护工具目录数据"
      section="admin-import-export"
    >
      <Card bordered={false} className="page-hero" style={{ marginBottom: 24 }}>
        <Space direction="vertical" size={12}>
          <Tag color="gold" style={{ width: 'fit-content', margin: 0 }}>Batch Operations</Tag>
          <Title level={2} style={{ margin: 0 }}>批量维护工具目录数据</Title>
        </Space>
      </Card>

      <Row gutter={[24, 24]}>
        <Col xs={24} xl={12}>
          <Card bordered={false} className="dashboard-card" title="导入工具数据">
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <Button onClick={() => void handleDownloadTemplate()}>下载模板</Button>
              <Upload
                beforeUpload={(file) => {
                  const reader = new FileReader();
                  reader.onload = async () => {
                    setCsvContent(String(reader.result ?? ''));
                    setCsvFileName(file.name);
                    message.success(`已载入 ${file.name}`);
                  };
                  reader.readAsText(file);
                  return false;
                }}
                accept=".csv"
                maxCount={1}
                showUploadList
              >
                <Button>选择 CSV 文件</Button>
              </Upload>
              <Space>
                <Button type="primary" style={{ background: '#183153' }} onClick={() => void handlePreview()} disabled={!csvContent}>
                  预校验
                </Button>
                <Button onClick={() => void handleImport()} disabled={!csvContent}>执行导入</Button>
              </Space>
              {preview ? (
                <Card size="small" style={{ background: '#fffdf7' }}>
                  <Space direction="vertical" size={8}>
                    <Text>总行数：{preview.totalRows}</Text>
                    <Text>有效行：{preview.validRows}</Text>
                    <Text>无效行：{preview.invalidRows}</Text>
                    {preview.errors.map((error) => (
                      <Text key={error} type="danger">{error}</Text>
                    ))}
                  </Space>
                </Card>
              ) : null}
            </Space>
          </Card>
        </Col>
        <Col xs={24} xl={12}>
          <Card bordered={false} className="dashboard-card" title="导出工具数据">
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <Checkbox.Group
                value={selectedFields}
                onChange={(values) => setSelectedFields(values as string[])}
                options={exportFieldOptions}
              />
              <Button type="primary" style={{ background: '#183153' }} onClick={() => void handleExport()}>
                生成导出文件
              </Button>
            </Space>
          </Card>
        </Col>
      </Row>

      <Row gutter={[24, 24]} style={{ marginTop: 8 }}>
        <Col xs={24} xl={12}>
          <Card bordered={false} className="dashboard-card" title="导入任务记录" extra={<Button onClick={() => void loadData()} loading={loading}>刷新</Button>}>
            <Table rowKey="id" columns={importColumns} dataSource={importTasks} pagination={{ pageSize: 5 }} />
          </Card>
        </Col>
        <Col xs={24} xl={12}>
          <Card bordered={false} className="dashboard-card" title="导出任务记录">
            <Table rowKey="id" columns={exportColumns} dataSource={exportTasks} pagination={{ pageSize: 5 }} />
          </Card>
        </Col>
      </Row>
    </EnterpriseShell>
  );
}
