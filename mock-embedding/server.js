const express = require('express');
const crypto = require('crypto');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());

function generateEmbedding(input) {
  const seed = typeof input === 'string' ? input : JSON.stringify(input);
  const hash = crypto.createHash('md5').update(seed).digest('hex');
  
  const dimensions = 1536;
  const embedding = [];
  
  for (let i = 0; i < dimensions; i++) {
    const value = parseInt(hash.substr((i % 32) * 2, 2), 16) / 255;
    embedding.push(Math.round((value * 2 - 1) * 10000) / 10000);
  }
  
  return embedding;
}

app.post('/v1/embeddings', (req, res) => {
  const { input, model = 'text-embedding-3-small' } = req.body;
  
  // 支持批量输入（数组）和单个输入
  const inputs = Array.isArray(input) ? input : [input];
  
  const data = inputs.map((text, index) => ({
    object: 'embedding',
    embedding: generateEmbedding(text),
    index: index
  }));
  
  const totalTokens = inputs.reduce((acc, text) => 
    acc + (typeof text === 'string' ? Math.ceil(text.length / 4) : 0), 0
  );
  
  res.json({
    object: 'list',
    data: data,
    model: model,
    usage: {
      prompt_tokens: totalTokens,
      total_tokens: totalTokens
    }
  });
});

// Mock Chat Completions API
app.post('/v1/chat/completions', (req, res) => {
  const { model = 'gpt-3.5-turbo', messages } = req.body;
  
  if (!messages || !Array.isArray(messages)) {
    return res.status(400).json({ error: { message: 'Invalid request' } });
  }
  
  const userMessage = messages.find(m => m.role === 'user');
  const content = userMessage ? userMessage.content : '';
  
  // 生成模拟回复
  let responseText = '这是一个模拟的AI回复。';
  
  if (content.includes('商品') || content.includes('描述') || content.includes('服装')) {
    // 提取商品名称
    const clothNameMatch = content.match(/商品名称[：:]?\s*(.+)/);
    const clothName = clothNameMatch ? clothNameMatch[1].trim() : '商品';
    
    responseText = `这款${clothName}是本季爆款单品！精选优质面料，舒适透气，经典版型百搭不挑人。时尚设计，彰显个性魅力，穿上它让你瞬间成为焦点！无论是日常通勤还是周末约会，都是你的绝佳选择。`;
  } else if (content.includes('评论') || content.includes('分析')) {
    responseText = `好评优点：
1. 质量不错
2. 款式新颖
3. 性价比高

差评缺点：
暂无明显差评

综合评价：该商品整体评价良好，用户反馈积极。`;
  } else {
    responseText = `您的消息：${content.substring(0, 50)}...\n\n这是模拟AI的回复内容。`;
  }
  
  res.json({
    id: 'chatcmpl-' + crypto.randomBytes(16).toString('hex'),
    object: 'chat.completion',
    created: Math.floor(Date.now() / 1000),
    model: model,
    choices: [{
      index: 0,
      message: {
        role: 'assistant',
        content: responseText
      },
      finish_reason: 'stop'
    }],
    usage: {
      prompt_tokens: Math.ceil(content.length / 4),
      completion_tokens: Math.ceil(responseText.length / 4),
      total_tokens: Math.ceil((content.length + responseText.length) / 4)
    }
  });
});

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

app.listen(PORT, () => {
  console.log(`Mock Embedding API running on port ${PORT}`);
});
