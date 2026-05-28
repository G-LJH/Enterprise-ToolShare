import type { NextApiRequest, NextApiResponse } from 'next';

export default function handler(req: NextApiRequest, res: NextApiResponse) {
  res.status(200).json({
    status: 'ok',
    service: 'tool-share-frontend',
    version: process.env.NEXT_PUBLIC_APP_VERSION || '0.1.0',
    timestamp: new Date().toISOString(),
  });
}
