import { useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import { App, Button, Card, Form, Input, Space, Typography } from 'antd';
import { apiRequest } from '../lib/api';
import { readAuthSession, writeAuthSession, type AuthSession } from '../lib/auth';

const { Title, Text } = Typography;

type LoginFormValues = {
  username: string;
  password: string;
};

export default function LoginPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<LoginFormValues>();

  useEffect(() => {
    if (readAuthSession()) {
      void router.replace('/tools');
    }
  }, [router]);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      const session = await apiRequest<AuthSession>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(values),
        skipAuth: true
      });
      writeAuthSession(session);
      message.success('登录成功');
      await router.push('/tools');
    } catch (error) {
      message.error(error instanceof Error ? error.message : '登录失败');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="login-shell" style={{ display: 'grid', placeItems: 'center', padding: '24px' }}>
      <Card bordered={false} className="login-form-card" style={{ width: '100%', maxWidth: 420, borderRadius: 24 }}>
        <Space direction="vertical" size={20} style={{ width: '100%' }}>
          <div style={{ textAlign: 'center' }}>
            <Text style={{ color: '#174b63', fontWeight: 700 }}>TOOL SHARE</Text>
            <Title level={2} style={{ marginTop: 8, marginBottom: 8 }}>登录</Title>
          </div>

          <Form form={form} layout="vertical">
            <Form.Item label="用户名" name="username" rules={[{ required: true, message: '请输入用户名' }]}>
              <Input size="large" autoComplete="username" />
            </Form.Item>
            <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}>
              <Input.Password size="large" autoComplete="current-password" />
            </Form.Item>
            <Button type="primary" size="large" block loading={submitting} onClick={handleSubmit} style={{ background: '#183153' }}>
              登录
            </Button>
          </Form>
        </Space>
      </Card>
    </div>
  );
}
