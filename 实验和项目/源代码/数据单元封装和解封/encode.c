#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "utils/protocols.h"
#include "utils/crc.h"
#include "utils/print.h"

/*
 ============================================================================
                                    应用层
 ============================================================================
*/
void application_layer_init(unsigned char *buffer, application_frame *app_layer, 
unsigned char *sender, unsigned short slen, 
unsigned char *receiver, unsigned short rlen, 
unsigned char *msg, unsigned short mlen)
{
    int tot = slen + rlen + mlen;
	// 将系统时间存储到应用层数据结构中 
    time_t timer = time(NULL);
	char* pctime = ctime(&timer);
	memcpy(app_layer->time, pctime, 24);
	// 将传入的数据长度存储到应用层数据结构中
    app_layer->slen = slen;
    app_layer->rlen = rlen;
    app_layer->mlen = mlen;
	app_layer->length = tot + Application_Frame_Length;
    // 动态分配内存以存储应用层数据，并将数据拷贝到新分配的内存中
	app_layer->sender = (char *)malloc(sizeof(char) * slen);
	app_layer->receiver = (char *)malloc(sizeof(char) * rlen);
	app_layer->msg = (char *)malloc(sizeof(char) * mlen);
	memcpy(app_layer->sender, sender, slen);
	memcpy(app_layer->receiver, receiver, rlen);
    memcpy(app_layer->msg, msg, mlen);
    
	//将应用层数据拷贝到指定的缓冲区中
	memcpy(buffer, sender, slen);
	memcpy(buffer + slen, receiver, rlen);
	memcpy(buffer + slen + rlen, msg, mlen);
	memcpy(buffer + tot, &app_layer->length, 2);
	memcpy(buffer + tot + 2, &slen, 2);
	memcpy(buffer + tot + 4, &rlen, 2);
	memcpy(buffer + tot + 6, &mlen, 2);
	memcpy(buffer + tot + 8, pctime, 24);
	memcpy(buffer + tot + 32,&app_layer->version, 2);
	memcpy(buffer + tot + 34,&app_layer->request, 2);
	memcpy(buffer + tot + 36,&app_layer->encode, 2);
	memcpy(buffer + tot + 38,&app_layer->state, 2);
}

/*
 ============================================================================
                                    传输层
 ============================================================================
*/
void udp_init(unsigned char *buffer, udp_frame *udp, unsigned short src_port, unsigned short dst_port, unsigned short tot, unsigned char src_ip[4], unsigned char dst_ip[4])
{
    // 设置 UDP 帧的各个字段
    udp->src_port = src_port;
    udp->dst_port = dst_port;
    udp->length = tot + 8; // UDP 长度包括头部和数据
    udp->checksum = 0;          // 先将校验和字段置零

    // 构建 UDP 伪首部（Pseudo Header）用于计算校验和
    unsigned char pesudo_header_udp[12 + 65515];
    unsigned char UDP_protocol = 11;          // UDP 协议号
    unsigned short UDP_length = tot + 8; // UDP 长度，包括头部和数据

    // 填充 UDP 伪首部
    memcpy(pesudo_header_udp, src_ip, 4);
    memcpy(pesudo_header_udp + 4, dst_ip, 4);
    pesudo_header_udp[8] = 0x00;
    memcpy(pesudo_header_udp + 9, &UDP_protocol, 1);
    memcpy(pesudo_header_udp + 10, &UDP_length, 2);

    // 将 UDP 帧头部和数据拷贝到 UDP 伪首部后面
    memcpy(pesudo_header_udp + 12, &udp->src_port, 2);
    memcpy(pesudo_header_udp + 14, &udp->dst_port, 2);
    memcpy(pesudo_header_udp + 16, &udp->length, 2);
    memcpy(pesudo_header_udp + 18, &udp->checksum, 2);
    memcpy(pesudo_header_udp + 20, buffer, tot);

    // 计算 UDP 校验和
    udp->checksum = Checksum(pesudo_header_udp, 12 + udp->length);

    // 将计算得到的校验和和其他字段写入缓冲区
    memcpy(buffer + tot, &udp->checksum, 2);
    memcpy(buffer + tot + 2, &udp->length, 2);
    memcpy(buffer + tot + 4, &udp->dst_port, 2);
    memcpy(buffer + tot + 6, &udp->src_port, 2);
}

/*
 ============================================================================
                                    网络层
 ============================================================================
*/
void ip_init(unsigned char *buffer, ipv4_frame *ip, unsigned char src_ip[4], unsigned char dst_ip[4], unsigned tot)
{
    // 设置 IPv4 帧的各个字段
    ip->version = 4;
    ip->header_length = 5;
    ip->type_of_service = 0;
    ip->total_length = 20 + tot; // IPv4 长度包括头部和数据
    ip->identification = 0;
    ip->flags = 0;
    ip->fragment_offset = 0;
    ip->time_to_live = 255;
    ip->protocol = 17;       // 指定上层协议为 UDP（17）
    ip->header_checksum = 0; // 先将校验和字段置零
    memcpy(ip->src_ip, src_ip, 4);
    memcpy(ip->dst_ip, dst_ip, 4);

    // 构建用于计算头部校验和的头部
    unsigned char checksum_header[20];
    unsigned char version_hdlen = (ip->version << 4) | ip->header_length;
    unsigned short flag_offset = (ip->flags << 13) | ip->fragment_offset;

    // 填充头部
    memcpy(checksum_header, &version_hdlen, 1);
    memcpy(checksum_header + 1, &ip->type_of_service, 1);
    memcpy(checksum_header + 2, &ip->total_length, 2);
    memcpy(checksum_header + 4, &ip->identification, 2);
    memcpy(checksum_header + 6, &flag_offset, 2);
    memcpy(checksum_header + 8, &ip->time_to_live, 1);
    memcpy(checksum_header + 9, &ip->protocol, 1);
    memcpy(checksum_header + 10, &ip->header_checksum, 2);
    memcpy(checksum_header + 12, src_ip, 4);
    memcpy(checksum_header + 16, dst_ip, 4);

    // 计算 IPv4 头部校验和
    ip->header_checksum = Checksum(checksum_header, 20);

    // 将 IPv4 头部和一些字段拷贝到指定的缓冲区中
    memcpy(buffer + tot, dst_ip, 4);
    memcpy(buffer + tot + 4, src_ip, 4);
    memcpy(buffer + tot + 8, &ip->header_checksum, 2);
    memcpy(buffer + tot + 10, &ip->protocol, 1);
    memcpy(buffer + tot + 11, &ip->time_to_live, 1);
    memcpy(buffer + tot + 12, &flag_offset, 2);
    memcpy(buffer + tot + 14, &ip->identification, 2);
    memcpy(buffer + tot + 16, &ip->total_length, 2);
    memcpy(buffer + tot + 18, &ip->type_of_service, 1);
    memcpy(buffer + tot + 19, &version_hdlen, 1);
}

/*
 ============================================================================
                                    数据链路层
 ============================================================================
*/
void data_link_layer_init(unsigned char *buffer, ethernet_frame *data_link, unsigned char src_mac[6], unsigned char dst_mac[6], unsigned tot)
{
    // 设置以太网帧的各个字段
    memcpy(data_link->src_mac, src_mac, 6);
    memcpy(data_link->dst_mac, dst_mac, 6);
    data_link->type[0] = 0x08; // 上层协议类型，0x0800 表示 IPv4
    data_link->type[1] = 0x00;
    data_link->crc = 0; // 先将 CRC 字段置零

    // 构建用于计算 CRC 的伪首部
    unsigned char pesudo_header[12 + 65535];
    memcpy(pesudo_header, src_mac, 6);
    memcpy(pesudo_header + 6, dst_mac, 6);
    memcpy(pesudo_header + 12, data_link->type, 2);
    memcpy(pesudo_header + 14, buffer, tot);

    // 计算 CRC32 校验和
    data_link->crc = crc32(pesudo_header, 14 + tot);

    // 将以太网帧的一些字段拷贝到指定的缓冲区中
    memcpy(buffer + tot, data_link->type, 2);
    memcpy(buffer + tot + 2, dst_mac, 6);
    memcpy(buffer + tot + 8, src_mac, 6);
    memcpy(buffer + tot + 14, &data_link->crc, 4);
}

int main()
{
    // 结构体定义
    application_frame app;
    udp_frame udp;
    ipv4_frame ip;
    ethernet_frame eth;

    unsigned char buffer[2000];
    unsigned char src_mac[6] = {0x00, 0x0c, 0x29, 0x1a, 0x2b, 0x3c};
    unsigned char dst_mac[6] = {0x00, 0x0c, 0x29, 0x1a, 0x2b, 0x3d};
    unsigned char src_ip[4] = {192, 168, 1, 1};
    unsigned char dst_ip[4] = {192, 168, 1, 2};
    unsigned short src_port = 8080;
    unsigned short dst_port = 8081;

    // 传输过程
    int tot = 0;
    unsigned char sender[20];
    unsigned char receiver[20];
    unsigned char msg[200];
    
    printf("消息内容: ");
    scanf("%[^\n]s",msg);
    getchar();
   	
	printf("发送给: ");
	scanf("%[^\n]s",receiver);
	getchar();
	
	printf("署名: ");
	scanf("%[^\n]s",sender); 

	app.version = 1;
	app.request = 0;
	app.encode = 0;
	app.state = 200;
    unsigned short slen = strlen(sender);
    unsigned short rlen = strlen(receiver);
    unsigned short mlen = strlen(msg);
	printf("================= 应用层封装 =================\n");
    application_layer_init(buffer, &app, sender, slen, receiver, rlen, msg, mlen);
    application_layer_print(&app);
	tot += slen + rlen + mlen + Application_Frame_Length;
	
    printf("================= UDP 封装 ====================\n");
    udp_init(buffer, &udp, src_port, dst_port, tot, src_ip, dst_ip);
    udp_print(&udp);
	tot += UDP_Frame_Length;
	
    printf("================= IPv4 封装 =================\n");
    ip_init(buffer, &ip, src_ip, dst_ip, tot);
    ip_print(&ip);
	tot += Ipv4_Frame_Length;

    printf("================= 数据链路层封装 =================\n");
    data_link_layer_init(buffer, &eth, src_mac, dst_mac, tot);
    data_link_layer_print(&eth);
	tot += Ethernet_Frame_Length;
	
    printf("数据链路传输成功!\n\n");
    printb(buffer,tot);

    // 数据链路结果打印
    FILE *outfile;
    outfile = fopen("./message.txt", "wb+");
    if (outfile == NULL)
    {
        printf("无法打开文件!\n");
    }
    printf("模拟封装总字节数: %d\n", tot);
    fwrite(buffer, sizeof(unsigned char), tot, outfile);
    fclose(outfile);
    
	
	
	return 0;
}
