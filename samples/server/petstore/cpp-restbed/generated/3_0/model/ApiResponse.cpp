/**
 * OpenAPI Petstore
 * This spec is mainly for testing Petstore server and contains fake endpoints, models. Please do not use this for any other purpose. Special characters: \" \\
 *
 * The version of the OpenAPI document: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI-Generator 7.15.0-SNAPSHOT.
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */



#include "ApiResponse.h"

#include <string>
#include <vector>
#include <map>
#include <sstream>
#include <stdexcept>
#include <regex>
#include <boost/lexical_cast.hpp>
#include <boost/property_tree/ptree.hpp>
#include <boost/property_tree/json_parser.hpp>
#include "helpers.h"

using boost::property_tree::ptree;
using boost::property_tree::read_json;
using boost::property_tree::write_json;

namespace org {
namespace openapitools {
namespace server {
namespace model {

ApiResponse::ApiResponse(boost::property_tree::ptree const& pt)
{
        fromPropertyTree(pt);
}


std::string ApiResponse::toJsonString(bool prettyJson /* = false */) const
{
	std::stringstream ss;
	write_json(ss, this->toPropertyTree(), prettyJson);
    // workaround inspired by: https://stackoverflow.com/a/56395440
    std::regex reg("\\\"([0-9]+\\.{0,1}[0-9]*)\\\"");
    std::string result = std::regex_replace(ss.str(), reg, "$1");
    return result;
}

void ApiResponse::fromJsonString(std::string const& jsonString)
{
	std::stringstream ss(jsonString);
	ptree pt;
	read_json(ss,pt);
	this->fromPropertyTree(pt);
}

ptree ApiResponse::toPropertyTree() const
{
	ptree pt;
	ptree tmp_node;
	pt.put("code", m_Code);
	pt.put("type", m_Type);
	pt.put("message", m_Message);
	return pt;
}

void ApiResponse::fromPropertyTree(ptree const &pt)
{
	ptree tmp_node;
	m_Code = pt.get("code", 0);
	m_Type = pt.get("type", "");
	m_Message = pt.get("message", "");
}

int32_t ApiResponse::getCode() const
{
    return m_Code;
}

void ApiResponse::setCode(int32_t value)
{
    m_Code = value;
}


std::string ApiResponse::getType() const
{
    return m_Type;
}

void ApiResponse::setType(std::string value)
{
    m_Type = value;
}


std::string ApiResponse::getMessage() const
{
    return m_Message;
}

void ApiResponse::setMessage(std::string value)
{
    m_Message = value;
}



std::vector<ApiResponse> createApiResponseVectorFromJsonString(const std::string& json)
{
    std::stringstream sstream(json);
    boost::property_tree::ptree pt;
    boost::property_tree::json_parser::read_json(sstream,pt);

    auto vec = std::vector<ApiResponse>();
    for (const auto& child: pt) {
        vec.emplace_back(ApiResponse(child.second));
    }

    return vec;
}

}
}
}
}

