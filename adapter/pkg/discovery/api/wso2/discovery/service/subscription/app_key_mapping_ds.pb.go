// Code generated by protoc-gen-go. DO NOT EDIT.
// versions:
// 	protoc-gen-go v1.25.0-devel
// 	protoc        v3.13.0
// source: wso2/discovery/service/subscription/app_key_mapping_ds.proto

package subscription

import (
	context "context"
	v3 "github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3"
	grpc "google.golang.org/grpc"
	codes "google.golang.org/grpc/codes"
	status "google.golang.org/grpc/status"
	protoreflect "google.golang.org/protobuf/reflect/protoreflect"
	protoimpl "google.golang.org/protobuf/runtime/protoimpl"
	reflect "reflect"
)

const (
	// Verify that this generated code is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(20 - protoimpl.MinVersion)
	// Verify that runtime/protoimpl is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(protoimpl.MaxVersion - 20)
)

var File_wso2_discovery_service_subscription_app_key_mapping_ds_proto protoreflect.FileDescriptor

var file_wso2_discovery_service_subscription_app_key_mapping_ds_proto_rawDesc = []byte{
	0x0a, 0x3c, 0x77, 0x73, 0x6f, 0x32, 0x2f, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79,
	0x2f, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2f, 0x73, 0x75, 0x62, 0x73, 0x63, 0x72, 0x69,
	0x70, 0x74, 0x69, 0x6f, 0x6e, 0x2f, 0x61, 0x70, 0x70, 0x5f, 0x6b, 0x65, 0x79, 0x5f, 0x6d, 0x61,
	0x70, 0x70, 0x69, 0x6e, 0x67, 0x5f, 0x64, 0x73, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x12, 0x1e,
	0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2e, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63,
	0x65, 0x2e, 0x73, 0x75, 0x62, 0x73, 0x63, 0x72, 0x69, 0x70, 0x74, 0x69, 0x6f, 0x6e, 0x1a, 0x2a,
	0x65, 0x6e, 0x76, 0x6f, 0x79, 0x2f, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2f, 0x64, 0x69,
	0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2f, 0x76, 0x33, 0x2f, 0x64, 0x69, 0x73, 0x63, 0x6f,
	0x76, 0x65, 0x72, 0x79, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x32, 0xab, 0x01, 0x0a, 0x25, 0x41,
	0x70, 0x70, 0x6c, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x4b, 0x65, 0x79, 0x4d, 0x61, 0x70,
	0x70, 0x69, 0x6e, 0x67, 0x44, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x53, 0x65, 0x72,
	0x76, 0x69, 0x63, 0x65, 0x12, 0x81, 0x01, 0x0a, 0x1c, 0x53, 0x74, 0x72, 0x65, 0x61, 0x6d, 0x41,
	0x70, 0x70, 0x6c, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x4b, 0x65, 0x79, 0x4d, 0x61, 0x70,
	0x70, 0x69, 0x6e, 0x67, 0x73, 0x12, 0x2c, 0x2e, 0x65, 0x6e, 0x76, 0x6f, 0x79, 0x2e, 0x73, 0x65,
	0x72, 0x76, 0x69, 0x63, 0x65, 0x2e, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2e,
	0x76, 0x33, 0x2e, 0x44, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x52, 0x65, 0x71, 0x75,
	0x65, 0x73, 0x74, 0x1a, 0x2d, 0x2e, 0x65, 0x6e, 0x76, 0x6f, 0x79, 0x2e, 0x73, 0x65, 0x72, 0x76,
	0x69, 0x63, 0x65, 0x2e, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2e, 0x76, 0x33,
	0x2e, 0x44, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e,
	0x73, 0x65, 0x22, 0x00, 0x28, 0x01, 0x30, 0x01, 0x42, 0x9d, 0x01, 0x0a, 0x34, 0x6f, 0x72, 0x67,
	0x2e, 0x77, 0x73, 0x6f, 0x32, 0x2e, 0x61, 0x70, 0x6b, 0x2e, 0x65, 0x6e, 0x66, 0x6f, 0x72, 0x63,
	0x65, 0x72, 0x2e, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79, 0x2e, 0x73, 0x65, 0x72,
	0x76, 0x69, 0x63, 0x65, 0x2e, 0x73, 0x75, 0x62, 0x73, 0x63, 0x72, 0x69, 0x70, 0x74, 0x69, 0x6f,
	0x6e, 0x42, 0x14, 0x41, 0x70, 0x70, 0x4b, 0x65, 0x79, 0x4d, 0x61, 0x70, 0x70, 0x69, 0x6e, 0x67,
	0x44, 0x53, 0x50, 0x72, 0x6f, 0x74, 0x6f, 0x50, 0x01, 0x5a, 0x4a, 0x67, 0x69, 0x74, 0x68, 0x75,
	0x62, 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 0x65, 0x6e, 0x76, 0x6f, 0x79, 0x70, 0x72, 0x6f, 0x78, 0x79,
	0x2f, 0x67, 0x6f, 0x2d, 0x63, 0x6f, 0x6e, 0x74, 0x72, 0x6f, 0x6c, 0x2d, 0x70, 0x6c, 0x61, 0x6e,
	0x65, 0x2f, 0x77, 0x73, 0x6f, 0x32, 0x2f, 0x64, 0x69, 0x73, 0x63, 0x6f, 0x76, 0x65, 0x72, 0x79,
	0x2f, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2f, 0x73, 0x75, 0x62, 0x73, 0x63, 0x72, 0x69,
	0x70, 0x74, 0x69, 0x6f, 0x6e, 0x88, 0x01, 0x01, 0x62, 0x06, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x33,
}

var file_wso2_discovery_service_subscription_app_key_mapping_ds_proto_goTypes = []interface{}{
	(*v3.DiscoveryRequest)(nil),  // 0: envoy.service.discovery.v3.DiscoveryRequest
	(*v3.DiscoveryResponse)(nil), // 1: envoy.service.discovery.v3.DiscoveryResponse
}
var file_wso2_discovery_service_subscription_app_key_mapping_ds_proto_depIdxs = []int32{
	0, // 0: discovery.service.subscription.ApplicationKeyMappingDiscoveryService.StreamApplicationKeyMappings:input_type -> envoy.service.discovery.v3.DiscoveryRequest
	1, // 1: discovery.service.subscription.ApplicationKeyMappingDiscoveryService.StreamApplicationKeyMappings:output_type -> envoy.service.discovery.v3.DiscoveryResponse
	1, // [1:2] is the sub-list for method output_type
	0, // [0:1] is the sub-list for method input_type
	0, // [0:0] is the sub-list for extension type_name
	0, // [0:0] is the sub-list for extension extendee
	0, // [0:0] is the sub-list for field type_name
}

func init() { file_wso2_discovery_service_subscription_app_key_mapping_ds_proto_init() }
func file_wso2_discovery_service_subscription_app_key_mapping_ds_proto_init() {
	if File_wso2_discovery_service_subscription_app_key_mapping_ds_proto != nil {
		return
	}
	type x struct{}
	out := protoimpl.TypeBuilder{
		File: protoimpl.DescBuilder{
			GoPackagePath: reflect.TypeOf(x{}).PkgPath(),
			RawDescriptor: file_wso2_discovery_service_subscription_app_key_mapping_ds_proto_rawDesc,
			NumEnums:      0,
			NumMessages:   0,
			NumExtensions: 0,
			NumServices:   1,
		},
		GoTypes:           file_wso2_discovery_service_subscription_app_key_mapping_ds_proto_goTypes,
		DependencyIndexes: file_wso2_discovery_service_subscription_app_key_mapping_ds_proto_depIdxs,
	}.Build()
	File_wso2_discovery_service_subscription_app_key_mapping_ds_proto = out.File
	file_wso2_discovery_service_subscription_app_key_mapping_ds_proto_rawDesc = nil
	file_wso2_discovery_service_subscription_app_key_mapping_ds_proto_goTypes = nil
	file_wso2_discovery_service_subscription_app_key_mapping_ds_proto_depIdxs = nil
}

// Reference imports to suppress errors if they are not otherwise used.
var _ context.Context
var _ grpc.ClientConnInterface

// This is a compile-time assertion to ensure that this generated file
// is compatible with the grpc package it is being compiled against.
const _ = grpc.SupportPackageIsVersion6

// ApplicationKeyMappingDiscoveryServiceClient is the client API for ApplicationKeyMappingDiscoveryService service.
//
// For semantics around ctx use and closing/ending streaming RPCs, please refer to https://godoc.org/google.golang.org/grpc#ClientConn.NewStream.
type ApplicationKeyMappingDiscoveryServiceClient interface {
	StreamApplicationKeyMappings(ctx context.Context, opts ...grpc.CallOption) (ApplicationKeyMappingDiscoveryService_StreamApplicationKeyMappingsClient, error)
}

type applicationKeyMappingDiscoveryServiceClient struct {
	cc grpc.ClientConnInterface
}

func NewApplicationKeyMappingDiscoveryServiceClient(cc grpc.ClientConnInterface) ApplicationKeyMappingDiscoveryServiceClient {
	return &applicationKeyMappingDiscoveryServiceClient{cc}
}

func (c *applicationKeyMappingDiscoveryServiceClient) StreamApplicationKeyMappings(ctx context.Context, opts ...grpc.CallOption) (ApplicationKeyMappingDiscoveryService_StreamApplicationKeyMappingsClient, error) {
	stream, err := c.cc.NewStream(ctx, &_ApplicationKeyMappingDiscoveryService_serviceDesc.Streams[0], "/discovery.service.subscription.ApplicationKeyMappingDiscoveryService/StreamApplicationKeyMappings", opts...)
	if err != nil {
		return nil, err
	}
	x := &applicationKeyMappingDiscoveryServiceStreamApplicationKeyMappingsClient{stream}
	return x, nil
}

type ApplicationKeyMappingDiscoveryService_StreamApplicationKeyMappingsClient interface {
	Send(*v3.DiscoveryRequest) error
	Recv() (*v3.DiscoveryResponse, error)
	grpc.ClientStream
}

type applicationKeyMappingDiscoveryServiceStreamApplicationKeyMappingsClient struct {
	grpc.ClientStream
}

func (x *applicationKeyMappingDiscoveryServiceStreamApplicationKeyMappingsClient) Send(m *v3.DiscoveryRequest) error {
	return x.ClientStream.SendMsg(m)
}

func (x *applicationKeyMappingDiscoveryServiceStreamApplicationKeyMappingsClient) Recv() (*v3.DiscoveryResponse, error) {
	m := new(v3.DiscoveryResponse)
	if err := x.ClientStream.RecvMsg(m); err != nil {
		return nil, err
	}
	return m, nil
}

// ApplicationKeyMappingDiscoveryServiceServer is the server API for ApplicationKeyMappingDiscoveryService service.
type ApplicationKeyMappingDiscoveryServiceServer interface {
	StreamApplicationKeyMappings(ApplicationKeyMappingDiscoveryService_StreamApplicationKeyMappingsServer) error
}

// UnimplementedApplicationKeyMappingDiscoveryServiceServer can be embedded to have forward compatible implementations.
type UnimplementedApplicationKeyMappingDiscoveryServiceServer struct {
}

func (*UnimplementedApplicationKeyMappingDiscoveryServiceServer) StreamApplicationKeyMappings(ApplicationKeyMappingDiscoveryService_StreamApplicationKeyMappingsServer) error {
	return status.Errorf(codes.Unimplemented, "method StreamApplicationKeyMappings not implemented")
}

func RegisterApplicationKeyMappingDiscoveryServiceServer(s *grpc.Server, srv ApplicationKeyMappingDiscoveryServiceServer) {
	s.RegisterService(&_ApplicationKeyMappingDiscoveryService_serviceDesc, srv)
}

func _ApplicationKeyMappingDiscoveryService_StreamApplicationKeyMappings_Handler(srv interface{}, stream grpc.ServerStream) error {
	return srv.(ApplicationKeyMappingDiscoveryServiceServer).StreamApplicationKeyMappings(&applicationKeyMappingDiscoveryServiceStreamApplicationKeyMappingsServer{stream})
}

type ApplicationKeyMappingDiscoveryService_StreamApplicationKeyMappingsServer interface {
	Send(*v3.DiscoveryResponse) error
	Recv() (*v3.DiscoveryRequest, error)
	grpc.ServerStream
}

type applicationKeyMappingDiscoveryServiceStreamApplicationKeyMappingsServer struct {
	grpc.ServerStream
}

func (x *applicationKeyMappingDiscoveryServiceStreamApplicationKeyMappingsServer) Send(m *v3.DiscoveryResponse) error {
	return x.ServerStream.SendMsg(m)
}

func (x *applicationKeyMappingDiscoveryServiceStreamApplicationKeyMappingsServer) Recv() (*v3.DiscoveryRequest, error) {
	m := new(v3.DiscoveryRequest)
	if err := x.ServerStream.RecvMsg(m); err != nil {
		return nil, err
	}
	return m, nil
}

var _ApplicationKeyMappingDiscoveryService_serviceDesc = grpc.ServiceDesc{
	ServiceName: "discovery.service.subscription.ApplicationKeyMappingDiscoveryService",
	HandlerType: (*ApplicationKeyMappingDiscoveryServiceServer)(nil),
	Methods:     []grpc.MethodDesc{},
	Streams: []grpc.StreamDesc{
		{
			StreamName:    "StreamApplicationKeyMappings",
			Handler:       _ApplicationKeyMappingDiscoveryService_StreamApplicationKeyMappings_Handler,
			ServerStreams: true,
			ClientStreams: true,
		},
	},
	Metadata: "wso2/discovery/service/subscription/app_key_mapping_ds.proto",
}
