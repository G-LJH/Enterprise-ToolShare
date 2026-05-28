import Link from 'next/link';
import { Button, Card, Col, Row, Space, Tag, Typography } from 'antd';

const { Title, Paragraph, Text } = Typography;

const quickActions = [
  { title: '进入工具目录', href: '/tools', note: '查找、筛选、收藏高频工具' },
  { title: '提交新工具', href: '/tools/submit', note: '补充团队正在使用的新工具' },
  { title: '查看工作流', href: '/workflows', note: '按场景复用成熟做法' },
  { title: '进入登录页', href: '/login', note: '使用内部账号进入管理与协作区' }
];

const highlights = [
  { title: '统一工具目录', desc: '团队共享工具入口、说明、标签和使用反馈。' },
  { title: '审核与治理', desc: '管理员在同一工作台完成审核、标签与日志治理。' },
  { title: '工作流沉淀', desc: '把零散工具串成场景化步骤，沉淀成团队方法。' },
  { title: '批量维护', desc: '支持导入导出，便于一次性整理已有工具资产。' }
];

export default function Home() {
  return (
    <div style={{ minHeight: '100vh', background: 'var(--page-bg)' }}>
      <div className="portal-header">
        <div className="portal-brand">
          <Text className="shell-eyebrow">TOOL SHARE</Text>
          <Title level={4} style={{ margin: 0, color: 'var(--ink)' }}>Enterprise Workspace</Title>
        </div>
        <nav className="portal-nav" aria-label="landing navigation">
          <Link className="portal-nav-link" href="/tools">工具目录</Link>
          <Link className="portal-nav-link" href="/tools/submit">提交工具</Link>
          <Link className="portal-nav-link" href="/workflows">工作流</Link>
          <Link className="portal-nav-link" href="/login">登录</Link>
        </nav>
      </div>

      <div style={{ maxWidth: 1280, margin: '0 auto', padding: '30px 24px 56px' }}>
        <Card bordered={false} className="portal-hero" bodyStyle={{ padding: 32 }}>
          <Row gutter={[28, 28]} align="middle">
            <Col xs={24} lg={15}>
              <Space direction="vertical" size={18} style={{ width: '100%' }}>
                <Tag color="gold" style={{ width: 'fit-content', margin: 0 }}>Internal Productivity Hub</Tag>
                <Title level={1} style={{ color: '#f8faf6', margin: 0 }}>
                  把团队常用工具、方法和流程放进一个统一入口
                </Title>
                <Paragraph style={{ color: 'rgba(248, 250, 246, 0.78)', fontSize: 16, marginBottom: 0 }}>
                  Space 面向企业内部协作，把工具目录、审核治理、使用反馈和工作流沉淀放进同一套操作界面。
                </Paragraph>
                <Space wrap size={12}>
                  <Button type="primary" size="large" style={{ background: '#d8ab57', color: '#12304f', fontWeight: 700 }}>
                    <Link href="/tools">进入工具目录</Link>
                  </Button>
                  <Button size="large" ghost style={{ color: '#f8faf6', borderColor: 'rgba(248, 250, 246, 0.44)' }}>
                    <Link href="/login">管理端登录</Link>
                  </Button>
                </Space>
              </Space>
            </Col>
            <Col xs={24} lg={9}>
              <Space direction="vertical" size={14} style={{ width: '100%' }}>
                <div className="portal-stat">
                  <Text style={{ color: 'rgba(248, 250, 246, 0.7)' }}>协作方式</Text>
                  <Title level={3} style={{ color: '#f8faf6', margin: '6px 0 0' }}>目录 + 反馈 + 审核 + 工作流</Title>
                </div>
                <div className="portal-stat">
                  <Text style={{ color: 'rgba(248, 250, 246, 0.7)' }}>适用团队</Text>
                  <Title level={3} style={{ color: '#f8faf6', margin: '6px 0 0' }}>产品、运营、研发、管理后台</Title>
                </div>
              </Space>
            </Col>
          </Row>
        </Card>

        <Row gutter={[20, 20]} style={{ marginTop: 24 }}>
          {quickActions.map((action) => (
            <Col xs={24} sm={12} lg={6} key={action.title}>
              <Card bordered={false} className="portal-grid-card">
                <Space direction="vertical" size={12}>
                  <Title level={4} style={{ margin: 0 }}>{action.title}</Title>
                  <Text type="secondary">{action.note}</Text>
                  <Button type="link" style={{ padding: 0, color: 'var(--accent)', fontWeight: 600 }}>
                    <Link href={action.href}>立即进入</Link>
                  </Button>
                </Space>
              </Card>
            </Col>
          ))}
        </Row>

        <Row gutter={[20, 20]} style={{ marginTop: 8 }}>
          {highlights.map((item) => (
            <Col xs={24} md={12} key={item.title}>
              <Card bordered={false} className="portal-grid-card">
                <Space direction="vertical" size={10}>
                  <Tag style={{ width: 'fit-content', margin: 0, background: 'var(--accent-soft)', color: 'var(--accent)', border: 'none' }}>
                    Feature
                  </Tag>
                  <Title level={4} style={{ margin: 0 }}>{item.title}</Title>
                  <Paragraph type="secondary" style={{ marginBottom: 0 }}>{item.desc}</Paragraph>
                </Space>
              </Card>
            </Col>
          ))}
        </Row>
      </div>
    </div>
  );
}
