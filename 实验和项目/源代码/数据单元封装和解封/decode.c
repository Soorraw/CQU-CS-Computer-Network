#include <stdio.h>
#include <string.h>
#include "utils/protocols.h"
#include "utils/crc.h"
#include "utils/print.h"

/*
 ============================================================================
                                    数据链路层
 ============================================================================
*/
void ethernet_receiver(unsigned char *buffer, ethernet_frame *ethernet_frame, unsigned short data_len)
{
    // 从接收缓冲区中提取以太网数据帧的相关字段
    memcpy(&ethernet_frame->crc, buffer + data_len - 4, 4);
    memcpy(&ethernet_frame->src_mac, buffer + data_len - 10, 6);
    memcpy(&ethernet_frame->dst_mac, buffer + data_len - 16, 6);
    memcpy(&ethernet_frame->type, buffer + data_len - 18, 2);

    // 构建伪头部用于计算 CRC 校验和
    unsigned char pesudo_header[65535];
    memcpy(pesudo_header, ethernet_frame->src_mac, 6);
    memcpy(pesudo_header + 6, ethernet_frame->dst_mac, 6);
    memcpy(pesudo_header + 12, ethernet_frame->type, 2);
    memcpy(pesudo_header + 14, buffer, data_len - 18);

    // 计算 CRC 校验和并与接收到的 CRC 进行比较
    unsigned int crc_result = crc32(pesudo_header, data_len - 4);
    unsigned int crc_received = ethernet_frame->crc;

    // 打印数据链路层解包结果
    printf("================= 数据链路层解包 =================\n");
    printf("\n");
	printb(buffer,data_len);
	if (crc_result == crc_received)
    {
        printf("- CRC 校验通过\n");
    }
    else
    {
        printf("- CRC 校验失败\n");
    }

    // 打印数据链路层帧内容
    data_link_layer_print(ethernet_frame);
}

/*
 ============================================================================
                                    网络层
 ============================================================================
*/
void ipv4_receiver(unsigned char *buffer, ipv4_frame *ip, unsigned short data_len)
{
    // 从接收缓冲区中提取版本和头部长度字段
    unsigned char version_hdlen = buffer[data_len - 1];
    ip->version = version_hdlen >> 4;
    ip->header_length = version_hdlen & 15;

    // 提取服务类型、总长度、标识、标志位和片偏移字段
    ip->type_of_service = buffer[data_len - 2];
    memcpy(&ip->total_length, buffer + data_len - 4, 2);
    memcpy(&ip->identification, buffer + data_len - 6, 2);
    unsigned short flag_offset;
    memcpy(&flag_offset, buffer + data_len - 8, 2);
    ip->flags = flag_offset >> 13;
    ip->fragment_offset = flag_offset & 8191;

    // 提取存活时间、协议、头部校验和字段
    ip->time_to_live = buffer[data_len - 9];
    ip->protocol = buffer[data_len - 10];
    memcpy(&ip->header_checksum, buffer + data_len - 12, 2);

    // 提取源和目标 IP 地址字段
    memcpy(ip->src_ip, buffer + data_len - 16, 4);
    memcpy(ip->dst_ip, buffer + data_len - 20, 4);

    // 构建用于计算头部校验和的头部
    unsigned char checksum_header[20];
    memcpy(checksum_header, &version_hdlen, 1);
    memcpy(checksum_header + 1, &ip->type_of_service, 1);
    memcpy(checksum_header + 2, &ip->total_length, 2);
    memcpy(checksum_header + 4, &ip->identification, 2);
    memcpy(checksum_header + 6, &flag_offset, 2);
    memcpy(checksum_header + 8, &ip->time_to_live, 1);
    memcpy(checksum_header + 9, &ip->protocol, 1);
    memcpy(checksum_header + 10, &ip->header_checksum, 2);
    memcpy(checksum_header + 12, &ip->src_ip, 4);
    memcpy(checksum_header + 16, &ip->dst_ip, 4);

    // 计算头部校验和并打印解包结果
    unsigned int checksum_result = Checksum(checksum_header, 20);
    printf("================= 网络层解包 =================\n");
    printf("\n");
	printb(buffer,data_len);
	printf("校验和: %d\n", checksum_result);
    if (checksum_result == 0)
    {
        printf("校验和校验通过!\n");
    }
    else
    {
        printf("校验和校验失败!\n");
    }

    // 打印网络层帧内容
    ip_print(ip);
}

/*
 ============================================================================
                                    传输层
 ============================================================================
*/
void udp_receiver(unsigned char *buffer, udp_frame *udp, unsigned short data_len, unsigned char src_ip[4], unsigned char dst_ip[4])
{
    // 从接收缓冲区中提取 UDP 数据帧字段
    memcpy(&udp->src_port, buffer + data_len - 2, 2);
    memcpy(&udp->dst_port, buffer + data_len - 4, 2);
    memcpy(&udp->length, buffer + data_len - 6, 2);
    memcpy(&udp->checksum, buffer + data_len - 8, 2);

    // 构建伪头部用于计算 UDP 校验和
    unsigned char pesudo_header[12 + 65535];
    memcpy(pesudo_header, src_ip, 4);
    memcpy(pesudo_header + 4, dst_ip, 4);
    pesudo_header[8] = 0x00;
    unsigned char UDP_protocol = 11;
    memcpy(pesudo_header + 9, &UDP_protocol, 1);
    memcpy(pesudo_header + 10, &udp->length, 2);
    memcpy(pesudo_header + 12, &udp->src_port, 2);
    memcpy(pesudo_header + 14, &udp->dst_port, 2);
    memcpy(pesudo_header + 16, &udp->length, 2);
    memcpy(pesudo_header + 18, &udp->checksum, 2);
    memcpy(pesudo_header + 20, buffer, data_len - 8);

    // 计算 UDP 校验和并打印解包结果
    unsigned int check_result = Checksum(pesudo_header, 12 + udp->length);
    printf("================= 传输层解包 =================\n");
    printf("\n");
	printb(buffer,data_len);    
	printf("校验和结果: %d\n", check_result);
    if (check_result == 0)
    {
        printf("校验和校验通过!\n");
    }
    else
    {
        printf("校验和校验失败!\n");
    }

    // 打印传输层帧内容
    udp_print(udp);
}

/*
 ============================================================================
                                    应用层
 ============================================================================
*/
void application_receiver(unsigned char *buffer, application_frame *app, unsigned short data_len)
{
  
    // 使用动态内存分配为数据字段分配空间并从接收缓冲区中拷贝数据
  	  
	memcpy(&app->length, buffer + data_len - 40, 2);
	memcpy(&app->slen, buffer + data_len - 38, 2);
	memcpy(&app->rlen, buffer + data_len - 36, 2);
	memcpy(&app->mlen, buffer + data_len - 34, 2);
	memcpy(&app->time, buffer + data_len - 32, 24); 	
	memcpy(&app->version, buffer + data_len - 8, 2);
	memcpy(&app->request, buffer + data_len - 6, 2);
	memcpy(&app->encode, buffer + data_len - 4, 2);
	memcpy(&app->state, buffer + data_len - 2, 2);
	
	app->sender = (char *)malloc(sizeof(char) * app->slen);
	app->receiver = (char *)malloc(sizeof(char) * app->rlen);
	app->msg = (char *)malloc(sizeof(char) * app->mlen);
	
	memcpy(app->sender, buffer, app->slen);
	memcpy(app->receiver, buffer + data_len - 40 - app->mlen - app->rlen, app->rlen);
	memcpy(app->msg, buffer + data_len - 40 - app->mlen, app->mlen);
 

    // 打印应用层解包结果
    printf("================= 应用层解包 =================\n");
    printf("\n");
	printb(buffer,data_len);
	application_layer_print(app);
}

int main()
{
    // 打开文件进行读取
    FILE *infile;
    infile = fopen("message.txt", "rb+");
    if (infile == NULL)
    {
        printf("无法打开文件!\n");
    }

    // 初始化接收缓冲区和数据长度
    unsigned char buffer[65535];
    int data_len = 0;
    unsigned char c;

    // 从文件读取数据到缓冲区
    for(;;)
    {
        c = fgetc(infile);
		if(feof(infile))
		break;
		buffer[data_len] = c;
        data_len++;
    }
    printf("接受文件总字节数: %d\n\n", data_len);

    // 关闭文件
    fclose(infile);

    // 结构体定义
    ethernet_frame ethernet_frame;
    ipv4_frame ipv4_frame;
    udp_frame udp_frame;
    application_frame application_frame;

    // 解包数据
    ethernet_receiver(buffer, &ethernet_frame, data_len);
    ipv4_receiver(buffer, &ipv4_frame, data_len - 18);
    udp_receiver(buffer, &udp_frame, data_len - 38, ipv4_frame.src_ip, ipv4_frame.dst_ip);
    application_receiver(buffer, &application_frame, data_len - 46);

    return 0;
}
