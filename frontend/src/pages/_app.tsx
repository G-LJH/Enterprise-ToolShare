import type { AppProps } from 'next/app';
import { App as AntdApp, ConfigProvider } from 'antd';
import 'antd/dist/reset.css';
import '../../styles/globals.css';

export default function App({ Component, pageProps }: AppProps) {
  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#183153',
          borderRadius: 14,
          colorBgLayout: '#f7f4ea',
          fontFamily: '"Avenir Next", "PingFang SC", "Hiragino Sans GB", sans-serif'
        }
      }}
    >
      <AntdApp>
        <Component {...pageProps} />
      </AntdApp>
    </ConfigProvider>
  );
}
