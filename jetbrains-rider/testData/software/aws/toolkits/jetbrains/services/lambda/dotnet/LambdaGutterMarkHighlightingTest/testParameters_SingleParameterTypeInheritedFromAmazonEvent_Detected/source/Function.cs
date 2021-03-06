using Amazon.Lambda.Core;
using Amazon.Lambda.APIGatewayEvents;

[assembly: LambdaSerializer(typeof(Amazon.Lambda.Serialization.Json.JsonSerializer))]

namespace HelloWorld
{
    public class Function
    {
        public APIGatewayProxyResponse FunctionHandler(MyCustomRequest apigProxyEvent)
        {
            return new APIGatewayProxyResponse();
        }
    }

    public class MyCustomRequest : APIGatewayProxyRequest
    {
    }
}