package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import org.apache.commons.io.FilenameUtils;
import org.csource.fastdfs.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


/**
 * http://api.gmall.com/admin/product/fileUpload
 * @author DuanYang
 * @create 2020-06-11 19:56
 */
@RestController
@RequestMapping("admin/product")
public class FileUploadController {
    @Value("${fileServer.url}")
    private String fileUrl;

    // 使用MultipartFile获取文件
    @RequestMapping("fileUpload")
    public Result<String> fileUpload(MultipartFile file) throws Exception {
        String configFile = this.getClass().getResource("/tracker.conf").getFile();

        String path = null;
        if (null!=configFile){
            //初始化client
            ClientGlobal.init(configFile);
            //创建trackerClient,trackerServer
            TrackerClient trackerClient = new TrackerClient();
            TrackerServer trackerServer = trackerClient.getConnection();
            //创建storageClient
            StorageClient1 storageClient1 = new StorageClient1(trackerServer,null);
            //获取文件后缀名 extension:扩展名
            String extName = FilenameUtils.getExtension(file.getOriginalFilename());
            path = storageClient1.upload_appender_file1(file.getBytes(), extName, null);
        }
        //拼接上传文件路径 http://192.168.200.128:8080
        return Result.ok(fileUrl + path);
    }
}
