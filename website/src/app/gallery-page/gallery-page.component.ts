import {Component, OnInit} from '@angular/core';

@Component({
  selector: 'app-gallery-page',
  templateUrl: './gallery-page.component.html',
  styleUrls: ['./gallery-page.component.less']
})
export class GalleryPageComponent implements OnInit {
  title = 'website';
  displayedColumns: string[] = ['name', 'reports'];
  projects = [
    {
      title: 'General',
      folder: 'general',
      projects: [
        {
          id: 'jenkins',
          name: 'Jenkins',
          size: '103 KLOC',
          description: "A free and open source automation server written in Java. Jenkins helps to automate the non-human part of the software development process, with continuous integration and facilitating technical aspects of continuous delivery. ",
          "logoLink": "data:image/png;base64,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",
          links: [
            {
              label: "https://jenkins.io/",
              href: "https://jenkins.io/"
            }
          ]
        },
        {
          id: "junit5",
          "name": "JUnit 5",
          size: "26 KLOC",
          "description": "JUnit 5 is the next generation of JUnit.",
          "logoLink": "data:image/png;base64,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",
          "links": [
            {
              "label": "GitHub Repo",
              "href": "https://github.com/junit-team/junit5"
            }
          ]
        },
        {
          id: "junit4",
          size: "11 KLOC",
          "name": "JUnit 4",
          "description": "JUnit is a unit testing framework for the Java programming language.",
          "logoLink": "data:image/png;base64,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",
          "links": [
            {
              "label": "GitHub Repo",
              "href": "https://github.com/junit-team/junit4"
            }
          ]
        },
        {
          "id": "node",
          "name": "Node.js",
          size: "130 KLOC",
          "description": "Node.js is a JavaScript runtime built on Chrome's V8 JavaScript engine.",
          "logoLink": "https://camo.githubusercontent.com/9c24355bb3afbff914503b663ade7beb341079fa/68747470733a2f2f6e6f64656a732e6f72672f7374617469632f696d616765732f6c6f676f2d6c696768742e737667",
          "links": [
            {
              "label": "GitHub Repo",
              "href": "https://github.com/nodejs/node"
            }
          ]
        },
        {
          "id": "php",
          size: "68 KLOC",
          "name": "PHP Interpreter",
          "description": "The PHP Interpreter https://www.php.net",
          "logoLink": "https://camo.githubusercontent.com/f7ca3c85e85a1a5b05ef1583af3921e6f4aea77e/68747470733a2f2f7777772e7068702e6e65742f696d616765732f6c6f676f732f6e65772d7068702d6c6f676f2e737667",
          "links": [
            {
              "label": "GitHub Repo",
              "href": "https://github.com/php/php-src"
            }
          ]
        },
        {
          "id": "spring-boot",
          "name": "Spring Boot",
          size: "108 KLOC",
          "description": "Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications that you can \"just run\".",
          "logoLink": "https://i2.wp.com/www.thecuriousdev.org/wp-content/uploads/2017/12/spring-boot-logo.png?w=200&ssl=1",
          "links": [
            {
              "label": "GitHub Repo",
              "href": "https://github.com/spring-projects/spring-boot"
            }
          ]
        },
        {
          id: "bitcoin",
          "name": "Bitcoin",
          size: "123 KLOC",
          "description": "A cryptocurrency, a decentralized digital currency without a central bank or single administrator that can be sent from user to user on the peer-to-peer bitcoin network without the need for intermediaries.",
          "logoLink": "https://en.bitcoin.it/w/images/en/2/29/BC_Logo_.png",
          "links": []
        },
        {
          id: "linux",
          size: "18 MLOC",
          "name": "Linux",
          "description": "a free and open-source, monolithic, Unix-like operating system kernel.",
          "logoLink": "https://upload.wikimedia.org/wikipedia/commons/thumb/3/35/Tux.svg/300px-Tux.svg.png",
          "links": []
        },
        {
          "id": "kubernetes",
          size: "1.5 MLOC",
          "name": "Kubernetes",
          "description": "An open source system for managing containerized applications across multiple hosts. It provides basic mechanisms for deployment, maintenance, and scaling of applications.",
          "logoLink": "https://upload.wikimedia.org/wikipedia/commons/thumb/3/39/Kubernetes_logo_without_workmark.svg/1920px-Kubernetes_logo_without_workmark.svg.png",
          "links": [
            {
              "label": "GitHub Repo",
              "href": "https://github.com/kubernetes/kubernetes"
            }
          ]
        },
        {
          id: "istio",
          "name": "Istio",
          size: "145 KLOC",
          "description": "An open platform to connect, manage, and secure microservices.",
          "logoLink": "https://istio.io/img/istio-whitelogo-bluebackground-framed.svg",
          "links": []
        },
        {
          "id": "thorntail",
          "name": "Thorntail",
          size: "45 KLOC",
          "description": "Thorntail provides a mechanism for building applications as uber jars, with just enough of the WildFly application server wrapped around it to support each application's use-case.",
          "logoLink": "https://developers.redhat.com/blog/wp-content/uploads/2019/05/Thorntail-logo-300x238.png",
          "links": [
            {
              "label": "GitHub Repo",
              "href": "https://github.com/thorntail/thorntail"
            }
          ]
        }
      ]
    },
    {
      title: "Apache",
      folder: "apache",
      projects: [
        {
          id: "httpd",
          "name": "Http Server (httpd)",
          size: "218 KLOC",
          "description": "A secure, efficient and extensible server that provides HTTP services in sync with the current HTTP standards.",
          "logoLink": "data:image/jpeg;base64,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",
          "links": [
            {
              "label": "Homepage",
              "href": "https://httpd.apache.org/"
            }
          ]
        },
        {
          id: "commons-lang",
          "name": "Commons Lang",
          size: "28 KLOC",
          "description": "The standard Java libraries fail to provide enough methods for manipulation of its core classes. Apache Commons Lang provides these extra methods.",
          "logoLink": "https://commons.apache.org/proper/commons-lang/images/commons-logo.png",
          "links": [
            {
              "label": "Homepage",
              "href": "https://commons.apache.org/proper/commons-lang/"
            }
          ]
        },
        {
          id: "kafka",
          "name": "Kafka",
          size: "189 KLOC",
          "description": "A unified, high-throughput, low-latency platform for handling real-time data feeds.",
          "logoLink": "data:image/png;base64,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",
          "links": [
            {
              "label": "Homepage",
              "href": "https://kafka.apache.org/"
            }
          ]
        }
      ]
    },
    {
      title: 'Uber OSS Projects',
      folder: 'ubeross',
      projects: [
        {
          name: 'All Uber OSS Projects',
          size: "836 KLOC",
          description: '',
          logoLink: 'https://uber.github.io/img/uber_os_logo.png',
          links: [],
          id: 'ALL'
        },
        {
          name: 'M3',
          size: "198 KLOC",
          description: 'The open source metrics platform built on M3DB, a distributed timeseries database.',
          logoLink: 'https://uber.github.io/img/m3-logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/m3db'
            }
          ],
          id: 'm3'
        },
        {
          name: 'Peloton',
          size: "114 KLOC",
          description: 'Unified Resource Scheduler to co-schedule mixed types of workloads for better resource utilization.',
          logoLink: 'https://uber.github.io/img/peloton_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/peloton'
            }
          ],
          id: 'peloton'
        },
        {
          name: 'Cadence',
          size: "104 KLOC",
          description: 'Orchestration engine used to develop applications which perform multiple actions over long periods of time.',
          logoLink: 'https://uber.github.io/img/cadence.svg',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/cadence'
            }
          ],
          id: 'cadence'
        },
        {
          name: 'Base Web',
          size: "89 KLOC",
          description: 'Base Web is a foundation for initiating, evolving, and unifying web products through a React component library.',
          logoLink: 'https://uber.github.io/img/base_web_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber-web/baseui/'
            }
          ],
          id: 'baseui'
        },
        {
          name: 'AresDB',
          size: "54 KLOC",
          description: 'AresDB is an efficient, GPU-powered real-time analytics storage and query engine.',
          logoLink: 'https://uber.github.io/img/aresdb_simple_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/aresdb'
            }
          ],
          id: 'aresdb'
        },
        {
          name: 'Jaeger Tracing',
          size: "44 KLOC",
          description: 'Jaeger, inspired by Dapper and OpenZipkin, is a distributed tracing system released by Uber.',
          logoLink: 'https://uber.github.io/img/jaeger_logo.svg',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/horovod/horovod'
            }
          ],
          id: 'jaeger'
        },
        {
          name: 'kepler.rl',
          size: "35 KLOC",
          description: 'kepler.gl is a data-agnostic, high-perf. web application for visual exploration of large-scale geolocation data sets.',
          logoLink: 'https://uber.github.io/img/kepler_gl_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/kepler.gl'
            }
          ],
          id: 'kepler.gl'
        },
        {
          name: 'Pyro',
          size: "34 KLOC",
          description: 'Pyro is a flexible, scalable deep probabilistic programming library built on PyTorch.',
          logoLink: 'https://uber.github.io/img/pyro_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/pyro/'
            }
          ],
          id: 'pyro'
        },
        {
          name: 'deck.gl',
          size: "37 KLOC",
          description: 'deck.gl is a WebGL-powered frame-work designed for exploring and visualizing data sets on-the-fly.',
          logoLink: 'https://uber.github.io/img/deck_gl_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/deck.gl'
            }
          ],
          id: 'deck.gl'
        },
        {
          name: 'Fusion.js',
          size: "19 KLOC",
          description: 'Fusion.js is a modern framework for fast, powerful React apps, which provides a rich set of tools.',
          logoLink: 'https://uber.github.io/img/fusionjs_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/fusionjs'
            }
          ],
          id: 'fusionjs'
        },
        {
          name: 'H3',
          size: "10 KLOC",
          description: 'A hexagonal hierarchical geospatial indexing system.',
          logoLink: 'https://uber.github.io/img/h3Logo-color.svg',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/h3'
            }
          ],
          id: 'h3'
        },
        {
          name: 'Horovod',
          size: "18 KLOC",
          description: 'Horovod is a distributed training framework for TensorFlow, Keras, PyTorch, and MXNet.',
          logoLink: 'https://uber.github.io/img/horovod_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/horovod/horovod'
            }
          ],
          id: 'horovod'
        },
        {
          name: 'Apache Hudi',
          size: "33 KLOC",
          description: 'Hudi (Hadoop Upserts anD Incremen-tals) is a spark library that is used to manage storage of large dataset.',
          logoLink: 'https://uber.github.io/img/hudi_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/hudi'
            }
          ],
          id: 'incubator-hudi'
        },

        {
          name: 'Kraken',
          size: "24 KLOC",
          description: 'P2P Docker registry capable of distributing TBs of data in seconds.',
          logoLink: 'https://uber.github.io/img/kraken.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/kraken'
            }
          ],
          id: 'kraken'
        },
        {
          name: 'Ludwig',
          size: "20 KLOC",
          description: 'A toolbox built on top of TensorFlow that allows to train and test deep learning models without writing code.',
          logoLink: 'https://uber.github.io/img/ludwig_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/ludwig'
            }
          ],
          id: 'ludwig'
        },
        {
          name: 'Makisu',
          size: "10 KLOC",
          description: 'Fast and flexible Docker image building tool, works in unprivileged containerized environments.',
          logoLink: 'https://uber.github.io/img/makisu.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/makisu'
            }
          ],
          id: 'makisu'
        },
        {
          name: 'Marmaray',
          size: "13 KLOC",
          description: 'Distributed any-source to any-sink data ingestion & dispersal framework used to ingest data in a Hadoop.',
          logoLink: 'https://uber.github.io/img/marmaray_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/marmaray'
            }
          ],
          id: 'marmaray'
        },
        {
          name: 'NullAway',
          size: "9 KLOC",
          description: 'A tool to help eliminate NullPointerExceptions in your Java code with low build-time overhead.',
          logoLink: 'https://uber.github.io/img/uber_os_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/NullAway'
            }
          ],
          id: 'NullAway'
        },

        {
          name: 'Petastorm',
          size: "4 KLOC",
          description: 'Data access library that enables single machine or distr. training and eval of DL models directly from datasets.',
          logoLink: 'https://uber.github.io/img/petastorm.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/petastorm'
            }
          ],
          id: 'petastorm'
        },

        {
          name: 'RIBs',
          size: "20 KLOC",
          description: 'RIBs is the cross-platform architecture framework behind many mobile apps at Uber.',
          logoLink: 'https://uber.github.io/img/rib_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/RIBs'
            }
          ],
          id: 'RIBs'
        },
        {
          name: 'AVS',
          size: "28 KLOC",
          description: 'Autonomous Visualization System (AVS) is a fast, powerful, web-based 3D visualization toolkit.',
          logoLink: 'https://uber.github.io/img/uber_os_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/streetscape.gl'
            }
          ],
          id: 'streetscape.gl'
        },
        {
          name: 'AutoDispose',
          size: "3 KLOC",
          description: 'AutoDispose is an RxJava 2 tool for automatically binding the execution of RxJava 2 streams to a provided scope.',
          logoLink: 'https://uber.github.io/img/uber_os_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/autodispose'
            }
          ],
          id: 'AutoDispose'
        },

        {
          name: 'CasualML',
          size: "3 KLOC",
          description: 'A Python Package for Uplift Modeling and Causal Inference with Machine Learning Algorithms.',
          logoLink: 'https://uber.github.io/img/causalml_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/causalml'
            }
          ],
          id: 'causalml'
        }, {
          name: 'Uber Poet',
          size: "1 KLOC",
          description: 'A mock swift project generator & build runner to help benchmark various module dependency graphs.',
          logoLink: 'https://uber.github.io/img/uber_os_logo.png',
          links: [
            {
              label: 'GitHub Repo',
              href: 'https://github.com/uber/uber-poet'
            }
          ],
          id: 'uber-poet'
        }
      ]
    }
  ];

  constructor() {
  }

  ngOnInit() {
  }

  reportsLink(folder, projectId) {
    return 'https://d3axxy9bcycpv7.cloudfront.net/'
      + folder + '/' + projectId + '/reports/html/index.html';
  }
}
