const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 7077, //前端接口
    proxy: {
      '/api': {
        target: 'http://localhost:8080',//后端接口
        changeOrigin: true,
        pathRewrite: {
          '^/api': '' //路径重写
        }
      }
    }
  },
  lintOnSave: false//关闭语法检查
})
