FROM java:8

MAINTAINER a_n_d_r_e_a_s_@_i_o_s_w_a_r_m_._d_e

USER root

RUN apt-get update && apt-get upgrade -y && apt-get install -y curl tar net-tools dnsutils

RUN mkdir -p /opt/oers/bin && mkdir -p /opt/oers/lib && mkdir -p /opt/oers/conf
ADD ./target/oers-core*.jar /opt/oers/lib
ADD ./target/lib/* /opt/oers/lib/
ADD oers.sh /opt/oers/bin
RUN chmod +x /opt/oers/bin/oers.sh

ENV OERS_HOME /opt/oers

CMD ["/opt/oers/bin/oers.sh"]

